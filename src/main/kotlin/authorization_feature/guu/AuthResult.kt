package org.example.authorization_feature.guu

import org.http4k.core.cookie.Cookie

sealed class AuthResult {
    data object WrongLoginOrPassword: AuthResult()
    data object WrongCaptcha: AuthResult()
    data object UnexpectedError: AuthResult()
    class UnknownResponseCode(val responseCode: Int): AuthResult()
    class Success(val cookies: List<Cookie>): AuthResult()
}