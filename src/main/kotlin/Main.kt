package org.example

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.tess4j.Tesseract
import org.example.authorization_feature.guu.AuthResult
import org.example.authorization_feature.guu.GuuAuthServiceImpl
import org.example.tables.ClassesTable
import org.example.tables.NewsTable
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

fun main() {

    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD")
    )

    val client = ApacheClient()
    val tesseract = Tesseract().also { it.setDatapath(tessdataDir) }
    val guuService = GuuServiceImpl(client)
    val guuAuthService = GuuAuthServiceImpl(tesseract)

    val api = routes(
        "classes" bind Method.GET to { r: Request ->
            r.query("g")?.let { group ->
                when (val dbResponse = ClassesTable.fetchClasses(group)) {
                    is DbResponse.Success -> Response(OK).body(Json.encodeToString(dbResponse.data.fillDatesGaps())).specifyContentType()
                    is DbResponse.Error -> Response(CONFLICT).body(dbResponse.message).specifyContentType()
                }
            } ?: Response(BAD_REQUEST).body("Group required")
        },
        "authorize" bind Method.GET to { r: Request ->
            r.header("c")?.let { cookie ->
                val group = guuService.fetchGroup(cookie = cookie) ?: return@let Response(CONFLICT).body("GUU server side error...")
                val classes = guuService.fetchClasses(cookie = cookie)

                ClassesTable.insertClasses(studentGroup = group, classObjects = classes)?.let {
                    Response(OK)
                } ?: return@let Response(CONFLICT).body("База калла")

                /*when(val dbResponse = ClassesTable.fetchClasses(group)) {
                    is DbResponse.Success -> {
                        Response(OK).body(Json.encodeToString(dbResponse.data))
                    }
                    is DbResponse.Error -> {
                        Response(CONFLICT).body("Произошла ошибка при получении введенных данных")
                    }
                }*/

            } ?: Response(BAD_REQUEST).body("Cookie required")
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
            val authResult = guuAuthService.procesAuth(login = userAuthData.login, password = userAuthData.password)

            when(authResult) {
                is AuthResult.Success -> {
                    Response(status = OK).body("Удачная авторизация! Куки: " + authResult.cookies.toString())
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
    )

    api.asServer(Jetty(9000)).start()

    println("SERVER STARTED")

    runBlocking {
        startRepeatableTimerTask(180) {
            guuService.fetchNews()
        }
    }
}