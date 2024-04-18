package org.example

import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.tess4j.Tesseract
import org.example.api.authorization.AuthorizationServiceImpl
import org.example.api.authorization.GuuWebsiteAuthResult
import org.example.api.authorization.GuuAuthServiceImpl
import api.authorization.security.AesEncryption
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.example.api.authorization.AuthResult
import org.example.authorization_feature.security.UserPasswordSecurity
import org.example.classes_feature.data.ClassObject
import org.example.classes_feature.guutt.ClassesService
import org.example.common.*
import org.example.common.results.GuuResponse
import org.example.tables.ClassesTable
import org.example.tables.NewsTable
import org.example.tables.UserDataTable
import org.example.tables.UsersTable
import org.example.tables.response.DbResponse
import org.example.tesseract.CaptchaServiceImpl
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

suspend fun main() = coroutineScope {

    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD")
    )

    val client = ApacheClient()
    val tesseract = Tesseract().also { it.setDatapath(tessdataDir) }
    val captchaService = CaptchaServiceImpl(tesseract)
    val guuService = GuuWebsiteServiceImpl(client)
    val aesEncryption = AesEncryption(
        keyStorePath = System.getenv("KS_PATH"),
        keyStorePassword = System.getenv("KS_PASS")
    )
    val guuAuthService = GuuAuthServiceImpl()
    val authService = AuthorizationServiceImpl(guuAuthService, captchaService)
    val classesService = ClassesService()

    val cachingService = CachingService(5000, 3L to TimeUnit.HOURS)

    val userPasswordSecurity = UserPasswordSecurity(aesEncryption, guuService)

    val api = routes(
        "classes" bind Method.GET to { r: Request ->
            r.query("g")?.let { group ->
                val cachedClasses = cachingService.getCache(group) as? Map<String, List<ClassObject>>
                if (cachedClasses == null) {
                    when(val dbResponse = ClassesTable.fetchGroupedClasses(group)) {
                        is DbResponse.Success -> {
                            // Залить в кэш
                            val classes = dbResponse.data.fillDatesGaps()
                            if (classes.isNotEmpty()) {
                                cachingService.putCache(group.trim(), classes)
                            }
                            Response(OK).body(Json.encodeToString(classes)).specifyContentType()
                        }
                        is DbResponse.Error -> {
                            Response(CONFLICT).body(dbResponse.message).specifyContentType()
                        }
                    }
                } else {
                    Response(OK).body(Json.encodeToString(cachedClasses)).specifyContentType()
                }
//                when (val dbResponse = ClassesTable.fetchGroupedClasses(group)) {
//                    is DbResponse.Success -> Response(OK).body(Json.encodeToString(dbResponse.data.fillDatesGaps())).specifyContentType()
//                    is DbResponse.Error -> Response(CONFLICT).body(dbResponse.message).specifyContentType()
//                }
            } ?: Response(BAD_REQUEST).body("Group required")
        },
        "groups" bind Method.GET to {
            when(val savedGroups = ClassesTable.getSavedGroups()) {
                is DbResponse.Success -> {
                    val data = savedGroups.data
                    Response(OK).body(Json.encodeToString(data)).specifyContentType()
                }
                is DbResponse.Error -> {
                    val error = savedGroups.message
                    Response(CONFLICT).body(error).specifyContentType()
                }
            }
        },
        "news" bind Method.GET to { _: Request ->
            when(val newsResponse = NewsTable.fetchAllNews()) {
                is DbResponse.Success -> {
                    Response(OK).body(Json.encodeToString(newsResponse.data)).specifyContentType()
                }
                is DbResponse.Error -> {
                    Response(CONFLICT).body(newsResponse.message).specifyContentType()
                }
            }
        },
        "auth" bind Method.POST to { request ->
            val userAuthData = Json.decodeFromString<UserAuthData>(request.bodyString())
            if (UsersTable.checkUserExistence(userAuthData.login) == true) {
                Response(status = CONFLICT).body("Пользователь уже зарегистрирован в системе")
            } else {
                launch {
                    val authResult = authService.authenticate(login = userAuthData.login, password = userAuthData.login)
                    when(authResult) {
                        is AuthResult.Success -> {
                            val cookies = authResult.cookies.stringify()
                            val isPassSaved = userPasswordSecurity.saveSecuredPassword(login = userAuthData.login, password = userAuthData.login, cookies = cookies)
                            if (isPassSaved) {
                                val group = userPasswordSecurity.getAndSaveUserInfo(login = userAuthData.login, cookies = cookies)
                                when(val classes = guuService.fetchClasses(cookies)) {
                                    is GuuResponse.Success -> {
                                        if (group != null) {
                                            classesService.updateScheduleOrNothing(group = group, classes.data)
                                            Response(OK)
                                        } else {
                                            Response(INTERNAL_SERVER_ERROR).body("Не удалось сохранить расписание")
                                        }
                                    }
                                    is GuuResponse.Forbidden -> {
                                        Response(INTERNAL_SERVER_ERROR).body("Не удалось получить распиание с сайта. FORBIDDEN")
                                    }
                                    is GuuResponse.NotResponding -> {
                                        Response(INTERNAL_SERVER_ERROR).body("Не удалось получить распиание с сайта. NOT RESPONDING")
                                    }
                                    is GuuResponse.CookieExpired -> {
                                        Response(INTERNAL_SERVER_ERROR).body("Не удалось получить распиание с сайта. COOKIE EXPIRED")
                                    }
                                }
                            } else {
                                Response(CONFLICT).body("Не удалось зарегистрировать пользователя. Код ошибки 111.")
                            }
                        }
                        is AuthResult.WrongLoginPassword -> {
                            Response(status = UNAUTHORIZED).body("Неверный логин или пароль.")
                        }
                        is AuthResult.ServerError -> {
                            Response(status = CONFLICT).body("Непредвиденная ошибка на сервере")
                        }
                        is AuthResult.WebsiteIsDown -> {
                            Response(status = CONFLICT).body("Проблемы с личным кабинетом ГУУ: ${authResult.details}")
                        }
                    }
                }
                Response(OK)
            }
        }
    )

    api.asServer(Jetty(9000)).start()

    println("SERVER STARTED")

    if (isProd) {
        launchRepeatingTask(720) {
            guuService.fetchNews()
        }

        /*launchRepeatingTask(1440) {
            val loginsGroupsResult = UserDataTable.getAllUsersLoginsGroups()
            if (loginsGroupsResult is DbResponse.Success) {
                loginsGroupsResult.data.distinctBy { it.group }.forEach { userLoginGroup ->
                    val cookiesResponse = UsersTable.getCookies(userLoginGroup.login)
                    if (cookiesResponse is DbResponse.Success) {
                        val guuClassesResponse = guuService.fetchClasses(cookiesResponse.data) // mapped and sorted
                        when(guuClassesResponse) {
                            is GuuResponse.Success -> {
                                classesService.updateScheduleOrNothing(userLoginGroup.group, guuClassesResponse.data)
                            }
                            is GuuResponse.CookieExpired -> {
                                // Need to reauthorize
                                val userPasswordResponse = UsersTable.getPassword(userLoginGroup.login)
                                if (userPasswordResponse is DbResponse.Success) {
                                    val securedPassword = userPasswordResponse.data
                                    // Need to add notification that keystore doesn't have this user secret key somehow.
                                    // Every usersList iteration will be skipped if keystore file is broken or empty, this is so stupid
                                    val decryptedPass = aesEncryption.decryptData(userLoginGroup.login, securedPassword) ?: return@forEach
                                    val authResponse = authService.authenticate(userLoginGroup.login, decryptedPass)
                                    when(authResponse) {
                                        is GuuWebsiteAuthResult.Success -> {
                                            val classesResponse = guuService.fetchClasses(authResponse.cookies.stringify())
                                            if (classesResponse is GuuResponse.Success) {
                                                classesService.updateScheduleOrNothing(userLoginGroup.group, classesResponse.data)
                                            }
                                        }
                                        is GuuWebsiteAuthResult.WrongLoginOrPassword -> {
                                            println("Какая-то ошибка с шифрованием. Возврат, что неверный пароль.")
                                        }
                                        is GuuWebsiteAuthResult.UnexpectedError -> {
                                            println("Неожиданная ошибка при авторизации")
                                        }
                                        is GuuWebsiteAuthResult.UnknownResponseCode -> {
                                            println("Странный код ответа: ${authResponse.responseCode}")
                                        }
                                        else -> {
                                            println("Неизвестная ошибка при авторизации")
                                        }
                                    }
                                }
                            }
                            is GuuResponse.Forbidden -> {
                                println("Не дает доступ")
                            }
                            is GuuResponse.NotResponding -> {
                                println("Не отвечает")
                            }
                        }
                    }
                }
            }
        }*/

        launchRepeatingTask(360) {
//            val groupAndCookie = UserDataTable.join(otherTable = UsersTable, joinType = JoinType.INNER, onColumn = UsersTable.)
            val (image, cookies) = guuAuthService.getCaptcha() ?: return@launchRepeatingTask
            val captchaResult = async {
                captchaService.solve(image)
            }

        }
    }
}
