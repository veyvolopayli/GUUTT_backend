package org.guutt.api.authorization

import api.authorization.security.AesEncryption
import api.tables.UserDetailsTable
import kotlinx.coroutines.*
import org.guutt.common.prefixForLoginCookies
import org.guutt.common.stringify
import org.guutt.logger
import api.tables.UsersTable
import org.guutt.GuuWebsiteService
import org.guutt.classes_feature.guutt.ClassesService
import org.guutt.common.encodeToBase64
import org.guutt.common.results.GuuResponse
import org.guutt.tesseract.CaptchaService
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.concurrent.atomic.AtomicInteger

class AuthorizationServiceImpl(
    private val guuWebsiteService: GuuWebsiteService,
    private val guuAuthService: GuuAuthService,
    private val captchaService: CaptchaService,
    private val aesEncryption: AesEncryption
) : AuthorizationService {
    private var countOfAuthFailures = AtomicInteger(0)
    val classesService = ClassesService(guuWebsiteService)

    /**
     * Производит авторизацию на my.guu.ru -> Шифрует и сохраняет регистрационные данные пользователя и куки ->
     * Получает и сохраняет детализацию пользователя -> Получает и сохраняет расписание
     */
    override suspend fun setupUserWithCommonData(login: String, password: String, alreadyRegistered: Boolean): AuthResult {
        val (captcha, captchaCookies) = withContext(Dispatchers.IO) {
            guuAuthService.getCaptcha()
        } ?: return AuthResult.ServerError
        val captchaResult = captchaService.solve(captcha) ?: return AuthResult.ServerError
        val authResult = guuAuthService.authorize(
            login = login,
            password = password,
            captcha = captchaResult,
            cookies = captchaCookies.stringify()
        )
        return when (authResult) {
            is GuuWebsiteAuthResult.Success -> {
                logger.info("Number of failed authorization attempts due to captcha: ${countOfAuthFailures.getAndSet(0)}")
                // Нужно объявить транзакцию для операций с БД
                newSuspendedTransaction transaction@{
                    coroutineScope {
                        val authCookies = authResult.cookies.stringify()
                        val savedUserGroup = fetchAndSaveUserDetails(login, authCookies) ?: return@coroutineScope AuthResult.ServerError
                        val isAuthDataSaved = async {
                            if (alreadyRegistered) {
                                encryptAndUpdateUserCookies(login = login, cookies = authCookies)
                            } else {
                                encryptAndSavePasswordWithCookies(login = login, password = password, cookies = authCookies)
                            } ?: false
                        }
                        val classesFetchedAndSaved = async {
                            classesService.fetchAndInsertOrUpdateClasses(
                                group = savedUserGroup,
                                cookies = authCookies
                            )
                        }
                        if (!isAuthDataSaved.await() || classesFetchedAndSaved.await() == null) {
                            rollback()
                            return@coroutineScope AuthResult.ServerError
                        }
                        return@coroutineScope AuthResult.Success(cookies = authResult.cookies, group = savedUserGroup)
                    }
//                    val authCookies = authResult.cookies.stringify()
//                    val isAuthDataSaved = if (alreadyRegistered) {
//                        encryptAndUpdateUserCookies(login = login, cookies = authCookies)
//                    } else {
//                        encryptAndSavePasswordWithCookies(login = login, password = password, cookies = authCookies)
//                    } ?: false
//                    val savedUserGroup = fetchAndSaveUserDetails(login, authCookies)
//                    if (!isAuthDataSaved || savedUserGroup == null) {
//                        rollback()
//                        return@transaction AuthResult.ServerError
//                    }
//
//                    AuthResult.Success(authResult.cookies, savedUserGroup)
                }
            }

            is GuuWebsiteAuthResult.WrongCaptcha -> {
                countOfAuthFailures.incrementAndGet()
                setupUserWithCommonData(login, password, alreadyRegistered)
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

    private suspend fun encryptAndSavePasswordWithCookies(login: String, password: String, cookies: String): Boolean? =
        coroutineScope {
            val encryptedPass = async {
                val passBytes = aesEncryption.encryptData(login, password)
                aesEncryption.encodeToBase64(passBytes)
            }
            val encryptedCookies = async {
                val cookiesBytes = aesEncryption.encryptData(prefixForLoginCookies(login), cookies)
                aesEncryption.encodeToBase64(cookiesBytes)
            }

            val insertedCount = UsersTable.insertOrUpdateUser(
                login = login,
                password = encryptedPass.await(),
                cookies = encryptedCookies.await()
            ) ?: return@coroutineScope null

            return@coroutineScope insertedCount > 0
        }

    private suspend fun encryptAndUpdateUserCookies(login: String, cookies: String): Boolean? {
        // Так как SecretKey от пароля уже хранится под ключом логина, нужно брать новый ключ, но относящийся к логину.
        val loginCookiesStorageKey = prefixForLoginCookies(login) // example@guu.ru_cookies
        val encryptedCookies = aesEncryption.encryptData(loginCookiesStorageKey, cookies).encodeToBase64()
        val updatedCount = UsersTable.updateUserCookies(login, encryptedCookies) ?: return null
        return updatedCount > 0
    }

    private fun fetchAndSaveUserDetails(login: String, cookies: String): String? {
        return when (val userInfo = guuWebsiteService.fetchUserDetails(cookies)) {
            is GuuResponse.Success -> {
                val insertedCount =
                    UserDetailsTable.insertOrUpdateUserDetails(login = login, userDetails = userInfo.data) ?: -1
                if (insertedCount > 0) userInfo.data.group else null
            }

            is GuuResponse.NotResponding -> {
                logger.error("$userInfo : ${userInfo.reason}")
                null
            }

            is GuuResponse.Forbidden -> {
                logger.error("$userInfo")
                null
            }

            is GuuResponse.CookieExpired -> {
                logger.info("$userInfo")
                null
            }
        }
    }
}