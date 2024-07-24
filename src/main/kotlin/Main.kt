package org.guutt

import api.authorization.security.AesEncryption
import api.tables.ClassesTable
import api.tables.UserDetailsTable
import api.tables.UsersTable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.tess4j.Tesseract
import org.guutt.api.authorization.AuthResult
import org.guutt.api.authorization.AuthorizationServiceImpl
import org.guutt.api.authorization.GuuAuthServiceImpl
import org.guutt.classes_feature.data.ClassObject
import org.guutt.classes_feature.guutt.ClassesService
import org.guutt.common.fillDatesGaps
import org.guutt.common.isProd
import org.guutt.common.specifyContentType
import org.guutt.common.tessdataDir
import org.guutt.tables.NewsTable
import org.guutt.tables.response.DbResponse
import org.guutt.tesseract.CaptchaServiceImpl
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit

val client = ApacheClient()
val tesseract = Tesseract().also { it.setDatapath(tessdataDir) }
val captchaService = CaptchaServiceImpl(tesseract)
val guuService = GuuWebsiteServiceImpl(client)
val aesEncryption = AesEncryption(
    keyStorePath = System.getenv("KS_PATH"),
    keyStorePassword = System.getenv("KS_PASS")
)
val guuAuthService = GuuAuthServiceImpl()
val guuWebsiteService = GuuWebsiteServiceImpl(ApacheClient())
val authService = AuthorizationServiceImpl(guuWebsiteService, guuAuthService, captchaService, aesEncryption)
val classesService = ClassesService(guuWebsiteService)

val cachingService = CachingService(5000, 3L to TimeUnit.HOURS)

val authHandler: HttpHandler = { request ->
    runBlocking {
        if (request.method != Method.POST) return@runBlocking Response(METHOD_NOT_ALLOWED)
        val userAuthData = try {
            Json.decodeFromString<UserAuthData>(request.bodyString())
        } catch (e: Exception) {
            logger.info("Произведен запрос на авторизацию с неправильным телом запроса")
            return@runBlocking Response(BAD_REQUEST).body("Неверное тело запроса")
        }

        if (UsersTable.checkUserExistence(userAuthData.login) == true) {
            return@runBlocking Response(status = CONFLICT).body("Пользователь уже зарегистрирован в системе")
        }

        val authResult = authService.setupUserWithCommonData(
            login = userAuthData.login,
            password = userAuthData.password,
            alreadyRegistered = false
        )

        when(authResult) {
            is AuthResult.Success -> {
                Response(OK).body(authResult.group)
            }
            is AuthResult.ServerError -> {
                Response(INTERNAL_SERVER_ERROR).body("Возникла ошибка на одном из этапов авторизации, повторите попытку позже.")
            }
            is AuthResult.WebsiteIsDown -> {
                Response(INTERNAL_SERVER_ERROR).body("Возникла проблема с доступом к my.guu.ru, возможно он лежит, повторите попытку позже.")
            }
            is AuthResult.WrongLoginPassword -> {
                Response(CONFLICT).body("Неправильный логин или пароль.")
            }
        }
    }
}

val groupClassesForCurrentSemesterHandler: HttpHandler = { request ->
    run {
        val group = request.query("g") ?: return@run Response(BAD_REQUEST).body("Group required")
        cachingService.getCache(group)?.let { classes ->
            return@run Response(OK).body(Json.encodeToString(classes as Map<String, List<ClassObject>>)).specifyContentType()
        }
        val (start, end) = currentSemester() ?: return@run Response(BAD_REQUEST).body("Сейчас не учебное время")
        val classesFromDb = ClassesTable.fetchGroupedClassesForPeriod(
                group, start, end)?.fillDatesGaps() ?: return@run Response(INTERNAL_SERVER_ERROR)
        cachingService.putCache(group, classesFromDb)
        return@run Response(OK).body(Json.encodeToString(classesFromDb)).specifyContentType()
    }
}

suspend fun main() = coroutineScope mainCoroutineScope@ {

    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD")
    )

    val api = routes(
        "classes" bind Method.GET to groupClassesForCurrentSemesterHandler,
        "groups" bind Method.GET to {
            when (val savedGroups = ClassesTable.getSavedGroups()) {
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
            when (val newsResponse = NewsTable.fetchAllNews()) {
                is DbResponse.Success -> {
                    Response(OK).body(Json.encodeToString(newsResponse.data.reversed())).specifyContentType()
                }

                is DbResponse.Error -> {
                    Response(CONFLICT).body(newsResponse.message).specifyContentType()
                }
            }
        },
        "auth" bind Method.POST to authHandler
    )

    api.asServer(Jetty(9000)).start()
    logger.info("SERVER STARTED")

    if (isProd) {
        // Обновление новостей с сайта раз в 12 часов
        launchRepeatingTask(60 * 12) {
            guuService.fetchNews()
        }

        // Обновление расписания для каждой группы из базы данных раз в 12 часов
        launchRepeatingTask(60 * 12) {
            val allUsersWithTheirGroupAndCookies = UserDetailsTable.selectAllUserGroupWithCookiesDecrypted(aesEncryption)
            allUsersWithTheirGroupAndCookies?.distinctBy { it.group }?.forEach { userGroupWithCookies ->
                launch {
                    val classes = transaction {
                        classesService.fetchAndInsertOrUpdateClasses(
                            group = userGroupWithCookies.group,
                            cookies = userGroupWithCookies.cookies
                        )
                    }
                    if (classes == null) {
                        // Пропускаем текущего пользователя, если не удалось получить пароль
                        val userPassword = UsersTable.getDecryptedPassword(userGroupWithCookies.login, aesEncryption) ?: return@launch
                        authService.setupUserWithCommonData(
                            login = userGroupWithCookies.login,
                            password = userPassword,
                            alreadyRegistered = true
                        )
                    }
                }
            }
            logger.info("All classes is up to date!")
        }
    }
}