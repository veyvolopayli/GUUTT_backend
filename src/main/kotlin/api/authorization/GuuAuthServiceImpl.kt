package org.example.api.authorization

import api.authorization.model.SimpleAuthDto
import api.authorization.model.SiteLogin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.common.*
import org.example.logger
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies
import java.io.File
import kotlin.random.Random
import kotlin.random.nextInt

class GuuAuthServiceImpl : GuuAuthService {
    override fun authorize(login: String, password: String, captcha: String, cookies: String): GuuWebsiteAuthResult {
        val authDto = SimpleAuthDto(
            SiteLogin(
                captcha = captcha,
                login = login,
                password = password
            )
        )

        val request = Request(Method.POST, GuuLinks.AUTH)
            .applyHeaders(contentType = "application/json")
            .cookieString(cookies)
            .body(Json.encodeToString(authDto))

        // If captcha is correct website will return 302 (Found), else 200 (OK)
        val response = ApacheClient().invoke(request)
        return when (response.status.code) {
            200 -> {
                // Captcha is NOT correct or login/password is not correct
                // If login/password isn't correct responseString() will contain "Проверьте правильность введенных данных."
                // If captcha is not correct -> "Неправильный проверочный код."
                // If both captcha and login/pass are not correct response will contain only wrong captcha message

                val loginPage = response.bodyString()
                when {
                    loginPage.contains(GuuAuthMessages.INCORRECT_CAPTCHA) -> GuuWebsiteAuthResult.WrongCaptcha
                    loginPage.contains(GuuAuthMessages.INCORRECT_LOGIN_PASSWORD) -> GuuWebsiteAuthResult.WrongLoginOrPassword
                    loginPage.contains(GuuAuthMessages.INCORRECT_EMAIL) -> GuuWebsiteAuthResult.WrongLoginOrPassword
                    else -> GuuWebsiteAuthResult.UnexpectedError
                }
            }

            302 -> {
                // Captcha and login/password are correct
                // We can use these cookies to access to all pages that need an authorization
                GuuWebsiteAuthResult.Success(response.cookies())
            }

            else -> {
                GuuWebsiteAuthResult.UnknownResponseCode(response.status.code)
            }
        }
    }

    override fun downloadCaptcha(): Pair<File, List<Cookie>>? {
        return try {
            val fileName = "img-${Random.nextInt(0..Int.MAX_VALUE)}.png"
            val request = Request(Method.GET, GuuLinks.CAPTCHA).applyHeaders("image/png")
            val response = ApacheClient().invoke(request)
            if (response.status.successful) {
                val stream = response.body.stream

                val imagesDir = File(captchaTempStorageDir).apply { mkdirs() }
                val file = File(imagesDir, fileName).also {
                    it.createNewFile()
                    it.writeBytes(stream.readAllBytes())
                    stream.close()
                }

                file to response.cookies()
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }
}