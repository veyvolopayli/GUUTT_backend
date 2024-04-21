package org.example.api.authorization

import api.authorization.security.AesEncryption
import kotlinx.coroutines.*
import org.example.common.getCookiesKeyForKeystore
import org.example.common.stringify
import org.example.logger
import api.tables.UsersTable
import org.example.tesseract.CaptchaService
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

class AuthorizationServiceImpl(private val guuAuthService: GuuAuthService, private val captchaService: CaptchaService, private val aesEncryption: AesEncryption) : AuthorizationService {
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
                // Зашифровать пароль и куки.
                // Сохранить их в бд.
                logger.info("Number of failed authorization attempts due to captcha: ${countOfAuthFailures.getAndSet(0)}")
                encryptAndSavePasswordWithCookies(login = login, password = password, cookies = cookies.stringify())
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

    private suspend fun encryptAndSavePasswordWithCookies(login: String, password: String, cookies: String): Boolean = coroutineScope {
        val encryptedPass = async {
            val passBytes = aesEncryption.encryptData(login, password)
            aesEncryption.encodeToBase64(passBytes)
        }
        val encryptedCookies = async {
            val cookiesBytes = aesEncryption.encryptData(getCookiesKeyForKeystore(login), cookies)
            aesEncryption.encodeToBase64(cookiesBytes)
        }

        (UsersTable.insertOrUpdateUser(
            login = login,
            password = encryptedPass.await(),
            cookies = encryptedCookies.await()
        ) ?: -1) > 0
    }

//    suspend fun reauthorizeExistingUser(login: String): Boolean = coroutineScope {
//        val password = async {
//            UsersTable.getDecryptedPassword(login, aesEncryption)
//        }
//        val cookies = async { UsersTable.getDecryptedCookies(login, aesEncryption) }
//        encryptAndSavePasswordWithCookies(login, password.await(), cookies.await())
//    }
}