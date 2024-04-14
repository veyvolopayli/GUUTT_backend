package org.example.api.authorization

import org.http4k.core.cookie.Cookie

sealed class AuthResult {
    class Success(val cookies: List<Cookie>) : AuthResult()
    data object WrongLoginPassword : AuthResult()
    data object ServerError : AuthResult()
    class WebsiteIsDown(val details: String? = null) : AuthResult()
}