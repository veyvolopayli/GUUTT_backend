package org.example.api.authorization

import org.example.common.stringify
import org.example.tesseract.CaptchaService

class AuthorizationServiceImpl(private val guuAuthService: GuuAuthService, private val captchaService: CaptchaService) : AuthorizationService {
    override fun authenticate(login: String, password: String): AuthResult {
        val (captchaFile, cookies) = guuAuthService.downloadCaptcha() ?: return AuthResult.ServerError
        val captchaResult = captchaService.solveAndDelete(captchaFile)
        val authResult = guuAuthService.authorize(
            login = login,
            password = password,
            captcha = captchaResult,
            cookies = cookies.stringify()
        )
        return when(authResult) {
            is GuuWebsiteAuthResult.Success -> {
                AuthResult.Success(authResult.cookies)
            }
            is GuuWebsiteAuthResult.WrongCaptcha -> {
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