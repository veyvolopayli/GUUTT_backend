package org.example.authorization_feature

import authorization_feature.model.AuthDto
import authorization_feature.model.SiteLogin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.applyHeaders
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.cookie.replaceCookie
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.random.Random

class AuthorizationRepositoryImpl(private val guuTokensService: GuuTokensService) : AuthorizationRepository {
    override fun getAuthPage(data: (htmlResponse: Response, captchaName: String) -> Unit) {
        val request = Request(Method.GET, "https://my.guu.ru/auth/login").applyHeaders("")
        val response = ApacheClient().invoke(request)

        val captchaUrl = response.bodyString().extractCaptchaImageUrl()

        val cookies = response.cookies()

        val captchaRequest = Request(Method.GET, "https://my.guu.ru$captchaUrl").applyHeaders("image/png")
            .cookie(cookies[0])
            .cookie(cookies[1])
            .cookie(cookies[2])

        val captchaResponse = ApacheClient().invoke(captchaRequest)

        val captchaResponseCookies = captchaResponse.cookies()

        println(captchaResponseCookies)

        val updatedResponse = response.replaceCookie(captchaResponseCookies[0])

        val captchaStream = captchaResponse.body.stream
        val rand = Random(100000).nextInt()

        val newFileName = "img$rand.png"

        File("py/$newFileName").apply {
            createNewFile()
            writeBytes(captchaStream.readAllBytes())
        }

        data.invoke(updatedResponse, newFileName)
    }

    override fun authorize(login: String, password: String) {
        getAuthPage { htmlResponse, captchaName ->
            guuTokensService.extractCsrfTokens(htmlResponse.bodyString()) { csrf, csrfToken ->
//                val captchaResult = recognizeCaptcha(captchaName)
                var captchaResult: String? = null
                while (captchaResult.isNullOrEmpty()) {
                    captchaResult = readlnOrNull()
                }

                val authDto = AuthDto(
                    siteLogin = SiteLogin(
                        captchaResult,
                        login,
                        password
                    ),
                    csrf = csrf,
                    csrfToken = csrfToken
                )

                val data = Json.encodeToString(authDto)

                println(data)

                println(htmlResponse.cookies())

                val request = Request(Method.POST, "https://my.guu.ru/auth/login")
                    .applyHeaders("application/json")
                    .cookie(htmlResponse.cookies()[0])
                    .cookie(htmlResponse.cookies()[1])
                    .cookie(htmlResponse.cookies()[2])
                    .body(data)

                println(request.cookies())
                println(data)

                val response = ApacheClient().invoke(request)
            }
        }
    }

    override fun recognizeCaptcha(fileName: String): String {
        return try {
            val pyScriptPath = "py/captcha_solver.py"
            val processBuilder = ProcessBuilder("py/venv/Scripts/python.exe", pyScriptPath, fileName)

            val process = processBuilder.start()

            println(fileName)

            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            var errorLine: String? = null
            while (errorReader.readLine().also { errorLine = it } != null) {
                println("Error: $errorLine")
            }

            val exitCode = process.waitFor()
            println(exitCode)

            val captcha = File("py/${fileName.replace(".png", ".txt")}").readText()

            println(captcha)
            captcha
        } catch (e: Exception) {
            println(e.stackTrace)
            ""
        }
    }

    override fun String.extractCaptchaImageUrl(): String {
        val document = Jsoup.parse(this)
        val imageElement = document.select("#sitelogin-captcha-image").first()
        return imageElement?.attr("src") ?: ""
    }
}

fun main() {
    val guuTokensService = GuuTokenServiceImpl()
    AuthorizationRepositoryImpl(guuTokensService).authorize("s121940@guu.ru", "emuxy3tr")
}
