package org.example

import authorization_feature.model.AuthDto
import authorization_feature.model.SiteLogin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.cookie.replaceCookie
import org.jsoup.Jsoup
import java.io.File
import kotlin.random.Random

class AuthorizationServiceImpl: AuthorizationService, CaptchaService {
    override fun getLoginPage(): Response {
        val request = Request(Method.GET, "https://my.guu.ru/auth/login").applyHeaders("")
        return ApacheClient().invoke(request)
    }

    override fun Response.extractDataForAuth(
        data: (captcha: String, csrf: String, csrfToken: String, newPHPSESSIDCookie: Cookie) -> Unit
    ) {
        val doc = Jsoup.parse(this.bodyString())

        val csrf = doc.selectFirst("meta[name=csrf-token]")?.attr("content")

        val csrfToken = doc.selectFirst("meta[name=csrf-token-value]")?.attr("content")

        if (csrf.isNullOrEmpty() || csrfToken.isNullOrEmpty()) throw Exception("Failed to parse document")

        val imageUrl = doc.select("#sitelogin-captcha-image").first()?.attr("src") ?: ""

        downloadCaptcha(url = imageUrl, cookies = this.cookies()) { captchaPath, newPHPSESSIDCookie ->
            val captcha = solveCaptcha(fileName = captchaPath)

            data.invoke(captcha, csrf, csrfToken, newPHPSESSIDCookie)
        }
    }

    override fun authorize(
        login: String,
        password: String
    ) {
        val loginPage = getLoginPage()
        loginPage.extractDataForAuth { captcha, csrf, csrfToken, newPhpsessidCookie ->
            val siteLogin = SiteLogin(
                captcha,
                login,
                password
            )
            val authDto = AuthDto(
                siteLogin = siteLogin,
                csrf = csrf,
                csrfToken = csrfToken
            )
            println(Json.encodeToString(authDto))
            println(loginPage.cookies())
            val loginPageUpdatedCookies = loginPage.replaceCookie(newPhpsessidCookie).cookies()
            println(loginPageUpdatedCookies)
            val request = Request(Method.POST, "https://my.guu.ru/auth/login")
                .applyHeaders(contentType = "application/json")
                .applyAuthCookies(cookies = loginPageUpdatedCookies)
                .body(Json.encodeToString(authDto))

            println("КУКИ AUTHORIZE" + request.cookies().toString())

            val response = ApacheClient().invoke(request)
            val responseCookies = response.cookies()
            println(response.bodyString())
            println(responseCookies)
        }
    }

    override fun downloadCaptcha(
        url: String, cookies: List<Cookie>, data: (captchaPath: String, newPHPSESSIDCookie: Cookie) -> Unit
    ) {
        val request = Request(Method.GET, "https://my.guu.ru$url")
            .applyHeaders("image/png").applyAuthCookies(cookies)

        val response = ApacheClient().invoke(request)

        val responsePHPSSIDCookie = response.cookies().first { it.name == "PHPSESSID" }

        val stream = response.body.stream
        val newFileName = "img_${Random.nextInt()}.png"
        val fileName = File("py/$newFileName").apply {
            createNewFile()
            writeBytes(stream.readAllBytes())
        }.name
        println(fileName)
        data.invoke(fileName, responsePHPSSIDCookie)
    }

    override fun solveCaptcha(fileName: String): String {
        //Запускаем py script, который решит капчу с вероятностью 50%
        val processBuilder = ProcessBuilder("py/venv/Scripts/python.exe", "py/captcha_solver.py", fileName)
        val process = processBuilder.start()

        val exitCode = process.waitFor()
        println(exitCode)

        if (exitCode != 0) return ""

        // После выхода из программы мы имеем .txt файл с решением капчи
        val txtFileName = fileName.replace(".png", ".txt")
        val captchaTxtFile = File("py/$txtFileName")
        val captcha = captchaTxtFile.readText().trim()

        return captcha
    }
}

fun main() {
    val authService = AuthorizationServiceImpl()
    authService.authorize(login = System.getenv("GUU_LOGIN"), password = System.getenv("GUU_PASSWORD"))
}