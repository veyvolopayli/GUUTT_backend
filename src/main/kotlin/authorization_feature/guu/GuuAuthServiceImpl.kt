package org.example.authorization_feature.guu

import authorization_feature.model.SiteLogin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.applyAuthCookies
import org.example.applyHeaders
import org.example.authorization_feature.model.SimpleAuthDto
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import java.io.File
import kotlin.random.Random

class GuuAuthServiceImpl : GuuAuthService, GuuCaptchaService {

    lateinit var cookieString: String
    override fun downloadCaptcha(data: (fileName: String, cookies: List<Cookie>) -> Unit) {
        val request = Request(Method.GET, "https://my.guu.ru/site/captcha").applyHeaders("image/png")

        val response = ApacheClient().invoke(request)

        val cookies = response.cookies()

        println("Куки с запроса капчи:" + cookies.toString())

        val cookieString = cookies.joinToString(";") {
            "${it.name}=${it.value}"
        }

        this.cookieString = cookieString

        println(cookieString)

        val stream = response.body.stream

        val randomId = Random.nextInt()

        val newFileName = "img$randomId.png"

        val fileName = File("py/$newFileName").apply {
            createNewFile()
            writeBytes(stream.readAllBytes())
        }.name

        data.invoke(fileName, cookies)
    }

    override fun authorize(login: String, password: String) {
        downloadCaptcha { fileName, cookies ->
            val captcha = solveCaptcha(fileName)
            /*var captcha: String? = null
            while (captcha.isNullOrEmpty()) {
                captcha = readlnOrNull()
            }*/
            val authDto = SimpleAuthDto(
                SiteLogin(
                    captcha = captcha,
                    login = login,
                    password = password
                )
            )

            println(Json.encodeToString(authDto))

//            val c = mutableListOf<Cookie>()


            val request = Request(Method.POST, "https://my.guu.ru/auth/login")
                .applyHeaders(contentType = "application/json")
                /*.cookie(cookies[0])
                .cookie(cookies[1])*/
                .header("Cookie", cookieString)
                .body(Json.encodeToString(authDto))

            val response = ApacheClient().invoke(request)
            val responseCookies = response.cookies()

            val responseCookieString = responseCookies.joinToString(";") {
                "${it.name}=${it.value}"
            }

            val studentRequest = Request(Method.GET, "https://my.guu.ru/student")
                .applyHeaders("text/html; charset=UTF-8")
                .header("Cookie", responseCookieString)

            val studentResponse = ApacheClient().invoke(studentRequest)
            val studentResponseCookies = studentResponse.cookies()

            println(studentResponse)
            println(studentResponseCookies)

            val classesRequest = Request(Method.GET, "https://my.guu.ru/student/classes")
                .applyHeaders("text/html; charset=UTF-8")
                .header("Cookie", responseCookieString)

            val classesResponse = ApacheClient().invoke(classesRequest)

            println(classesResponse)
        }
    }

    override fun solveCaptcha(fileName: String): String {
        val processBuilder = ProcessBuilder("py/venv/Scripts/python.exe", "py/captcha_solver.py", fileName)
        val process = processBuilder.start()

        // Ожидаем завершения работы скрипта и получаем exitCode
        val exitCode = process.waitFor()
        if (exitCode != 0) return ""

        // После выхода из программы мы имеем .txt файл с решением капчи
        val txtFileName = fileName.replace(".png", ".txt")
        val captchaTxtFile = File("py/$txtFileName")
        val captcha = captchaTxtFile.readText().trim()

        return captcha
    }
}

fun main() {
    val authService = GuuAuthServiceImpl()
    authService.authorize(login = System.getenv("GUU_LOGIN"), password = System.getenv("GUU_PASSWORD"))
}