package org.example

import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.tess4j.Tesseract
import org.example.authorization_feature.guu.AuthResult
import org.example.authorization_feature.guu.GuuAuthServiceImpl
import org.example.authorization_feature.security.AesEncryption
import org.example.classes_feature.guu.ClassesService
import org.example.tables.ClassesTable
import org.example.tables.NewsTable
import org.example.tables.UserDataTable
import org.example.tables.UsersTable
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database

suspend fun main() = coroutineScope {

    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD")
    )

    val client = ApacheClient()
    val tesseract = Tesseract().also { it.setDatapath(tessdataDir) }
    val guuService = GuuWebsiteServiceImpl(client)
    val aesEncryption = AesEncryption(
        keyStorePath = System.getenv("KS_PATH"),
        keyStorePassword = System.getenv("KS_PASS")
    )
    val guuAuthService = GuuAuthServiceImpl(tesseract)
    val classesService = ClassesService()

    val api = routes(
        "classes" bind Method.GET to { r: Request ->
            r.query("g")?.let { group ->
                when (val dbResponse = ClassesTable.fetchGroupedClasses(group)) {
                    is DbResponse.Success -> Response(OK).body(Json.encodeToString(dbResponse.data.fillDatesGaps())).specifyContentType()
                    is DbResponse.Error -> Response(CONFLICT).body(dbResponse.message).specifyContentType()
                }
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
                val authResult = guuAuthService.procesAuth(login = userAuthData.login, password = userAuthData.password)
                when(authResult) {
                    is AuthResult.Success -> {
                        val authCookies = authResult.cookies.stringify()
                        val securedPass = aesEncryption.encryptPassword(userAuthData.login, userAuthData.password)
                        val userInfoResponse = guuService.getUserInfo(authCookies)
                        if (userInfoResponse is GuuResponse.Success) {
                            val insertDataResponse = UsersTable.insertData(userAuthData.login, aesEncryption.encode(securedPass), authCookies)
                            if (insertDataResponse is DbResponse.Error) {
                                UsersTable.updateCookies(userAuthData.login, authCookies)
                            }
                            UserDataTable.insertData(userAuthData.login, userInfoResponse.data)
                            // Добавление расписания
                            val guuClassesResponse = guuService.fetchClasses(authCookies)
                            if (guuClassesResponse is GuuResponse.Success) {
                                classesService.updateScheduleOrNothing(userInfoResponse.data.group, guuClassesResponse.data)
                                Response(status = OK).body("Удача!")
                            } else {
                                Response(status = CONFLICT).body("Не удалось получить расписание с сайта.")
                            }
                        } else {
                            Response(status = CONFLICT).body("Возникла ошибка на одном из этапов авторизации. Повторите попытку!")
                        }
                    }
                    is AuthResult.WrongLoginOrPassword -> {
                        Response(status = UNAUTHORIZED).body("Неверный логин или пароль.")
                    }
                    is AuthResult.UnexpectedError -> {
                        Response(status = CONFLICT).body("Непредвиденная ошибка")
                    }
                    is AuthResult.UnknownResponseCode -> {
                        Response(status = CONFLICT).body("Неожиданный код ответа: ${authResult.responseCode}")
                    }
                    else -> {
                        Response(status = CONFLICT).body("Пу пу пууу...")
                    }
                }
            }
        }
    )

    api.asServer(Jetty(9000)).start()

    println("SERVER STARTED")

    launchRepeatingTask(720) {
        guuService.fetchNews()
    }

    launchRepeatingTask(1440) {
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
                                val passwordBytes = aesEncryption.decode(securedPassword) // Get byteArray from base64 password string
                                // Need to add notification that keystore doesn't have this user secret key somehow.
                                // Every usersList iteration will be skipped if keystore file is broken or empty, this is so stupid
                                val decryptedPass = aesEncryption.decryptPassword(userLoginGroup.login, passwordBytes) ?: return@forEach
                                val authResponse = guuAuthService.procesAuth(userLoginGroup.login, decryptedPass)
                                when(authResponse) {
                                    is AuthResult.Success -> {
                                        val classesResponse = guuService.fetchClasses(authResponse.cookies.stringify())
                                        if (classesResponse is GuuResponse.Success) {
                                            classesService.updateScheduleOrNothing(userLoginGroup.group, classesResponse.data)
                                        }
                                    }
                                    is AuthResult.WrongLoginOrPassword -> {
                                        println("Какая-то ошибка с шифрованием. Возврат, что неверный пароль.")
                                    }
                                    is AuthResult.UnexpectedError -> {
                                        println("Неожиданная ошибка при авторизации")
                                    }
                                    is AuthResult.UnknownResponseCode -> {
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
    }
}