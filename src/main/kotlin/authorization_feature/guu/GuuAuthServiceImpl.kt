package org.example.authorization_feature.guu

import authorization_feature.model.SiteLogin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.*
import org.example.authorization_feature.model.SimpleAuthDto
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies
import java.io.File
import java.time.Instant
import kotlin.random.Random
import kotlin.random.nextInt

class GuuAuthServiceImpl : GuuAuthService, GuuCaptchaServiceImpl() {

    private fun downloadCaptcha(fileName: String): List<Cookie> {
        val request = Request(Method.GET, GuuLinks.CAPTCHA).applyHeaders("image/png")
        val response = ApacheClient().invoke(request)
        val stream = response.body.stream
        val cookies = response.cookies()

        File("py/$fileName").apply {
            createNewFile()
            writeBytes(stream.readAllBytes())
        }

        return cookies
    }

    private fun authorize(login: String, password: String, captcha: String, cookies: String): AuthResult {
        val authDto = SimpleAuthDto(
            SiteLogin(
                captcha = captcha,
                login = login,
                password = password
            )
        )

        println("new try")

        val request = Request(Method.POST, GuuLinks.AUTH)
            .applyHeaders(contentType = "application/json")
            .cookieString(cookies)
            .body(Json.encodeToString(authDto))

        // If captcha is correct api returning 302 Found, else 200 OK
        val response = ApacheClient().invoke(request)

        return when (response.status.code) {
            200 -> {
                // Captcha is NOT correct or login/password is not correct
                // If login/password isn't correct responseString() will contain "Проверьте правильность введенных данных."
                // If captcha is not correct -> "Неправильный проверочный код."
                // If both captcha and login/pass are not correct response will contain only wrong captcha message

                if (response.bodyString().contains(GuuAuthMessages.INCORRECT_CAPTCHA)) {
                    AuthResult.WrongCaptcha
                } else if (response.bodyString().contains(GuuAuthMessages.INCORRECT_LOGIN_PASSWORD)) {
                    AuthResult.WrongLoginOrPassword
                } else {
                    AuthResult.UnexpectedError
                }
            }
            302 -> {
                // Captcha and login/password are correct
                // We can use these cookies to access to all pages that need an authorization
                AuthResult.Success(response.cookies())
            }
            else -> {
                AuthResult.UnknownResponseCode(response.status.code)
            }
        }
    }

    private fun generateCaptchaName() = "img_${Random.nextInt(0..Int.MAX_VALUE)}.png"

    override fun procesAuth(login: String, password: String): AuthResult {
        val captchaName = generateCaptchaName()

        var authResult: AuthResult = AuthResult.WrongCaptcha

        while (authResult is AuthResult.WrongCaptcha) {
            val cookies = downloadCaptcha(captchaName)
            val captcha = solveCaptcha(captchaName)
            authResult = authorize(
                login = login,
                password = password,
                captcha = captcha,
                cookies = cookies.stringify()
            )
        }

        return authResult
    }
}

fun main() {
    val authService = GuuAuthServiceImpl()

    val result = authService.procesAuth(
        login = System.getenv("GUU_LOGIN"),
        password = System.getenv("GUU_PASSWORD")
    )

    println("ФИНАЛЬНЫЙ РЕЗУЛЬТАТ: ${result.cookies[0]}")
}

/*val classesRequest = Request(Method.GET, GuuLinks.CLASSES)
                .applyHeaders("text/html; charset=UTF-8")
                .cookieString(authResponseCookieString)

            val classesResponse = ApacheClient().invoke(classesRequest)

            println(classesResponse)*/



/*val studentRequest = Request(Method.GET, "https://my.guu.ru/student")
    .applyHeaders("text/html; charset=UTF-8")
    .header("Cookie", authResponseCookieString)

val studentResponse = ApacheClient().invoke(studentRequest)
val studentResponseCookiesString = studentResponse.cookiesString()

println(studentResponse)
println(studentResponseCookiesString)

val classesRequest = Request(Method.GET, GuuLinks.CLASSES)
    .applyHeaders("text/html; charset=UTF-8")
    .applyAuthCookies(studentResponseCookies)

val classesResponse = ApacheClient().invoke(classesRequest)

println(classesResponse)*/