package org.example

import org.http4k.core.Response
import org.http4k.core.cookie.Cookie

interface AuthorizationService {
    fun getLoginPage(): Response
    fun Response.extractDataForAuth(data: (captcha: String, csrf: String, csrfToken: String, newPHPSESSIDCookie: Cookie) -> Unit)
    fun authorize(login: String, password: String)
}