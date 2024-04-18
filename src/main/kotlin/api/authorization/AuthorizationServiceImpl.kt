package org.example.api.authorization

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.common.stringify
import org.example.logger
import org.example.tesseract.CaptchaService
import java.util.concurrent.atomic.AtomicInteger

class AuthorizationServiceImpl(private val guuAuthService: GuuAuthService, private val captchaService: CaptchaService) : AuthorizationService {
    private var countOfAuthFailures = AtomicInteger(0)
    override suspend fun authenticate(login: String, password: String): AuthResult {
        val (captcha, cookies) = withContext(Dispatchers.IO) { guuAuthService.getCaptcha() } ?: return AuthResult.ServerError
        val captchaResult = captchaService.solve(captcha) ?: return AuthResult.ServerError
        val authResult = guuAuthService.authorize(
            login = login,
            password = password,
            captcha = captchaResult,
            cookies = cookies.stringify()
        )
        return when(authResult) {
            is GuuWebsiteAuthResult.Success -> {
                logger.info("Number of failed authorization attempts due to captcha: ${countOfAuthFailures.getAndSet(0)}")
                AuthResult.Success(authResult.cookies)
            }
            is GuuWebsiteAuthResult.WrongCaptcha -> {
                countOfAuthFailures.incrementAndGet()
                authenticate(login, password)
            }
            is GuuWebsiteAuthResult.WrongLoginOrPassword -> {
                AuthResult.WrongLoginPassword
            }
            is GuuWebsiteAuthResult.UnknownResponseCode -> {
                AuthResult.WebsiteIsDown("Сайт вернул ${authResult.responseCode} код")
            }
            is GuuWebsiteAuthResult.UnexpectedError -> {
                AuthResult.ServerError
            }
        }
    }
}