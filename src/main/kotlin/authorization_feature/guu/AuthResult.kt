package org.example.authorization_feature.guu

import org.http4k.core.cookie.Cookie

sealed class AuthResult(val cookies: List<Cookie> = emptyList(), val responseCode: Int = 0) {
    data object WrongLoginOrPassword: AuthResult()
    data object WrongCaptcha: AuthResult()
    data object UnexpectedError: AuthResult()
    class UnknownResponseCode(responseCode: Int): AuthResult(responseCode = responseCode)
    class Success(cookies: List<Cookie>): AuthResult(cookies = cookies)
}