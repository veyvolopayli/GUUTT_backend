package org.guutt.api.authorization

import org.http4k.core.cookie.Cookie

sealed class AuthResult {
    class Success(val cookies: List<Cookie>, val group: String) : AuthResult()
    data object WrongLoginPassword : AuthResult()
    data object ServerError : AuthResult()
    class WebsiteIsDown(val details: String? = null) : AuthResult()
}