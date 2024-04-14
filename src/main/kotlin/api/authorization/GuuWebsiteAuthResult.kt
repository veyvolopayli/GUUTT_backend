package org.example.api.authorization

import org.http4k.core.cookie.Cookie

sealed class GuuWebsiteAuthResult {
    data object WrongLoginOrPassword: GuuWebsiteAuthResult()
    data object WrongCaptcha: GuuWebsiteAuthResult()
    data object UnexpectedError: GuuWebsiteAuthResult()
    class UnknownResponseCode(val responseCode: Int): GuuWebsiteAuthResult()
    class Success(val cookies: List<Cookie>): GuuWebsiteAuthResult()
}