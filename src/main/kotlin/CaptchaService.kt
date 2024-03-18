package org.example

import org.http4k.core.Response
import org.http4k.core.cookie.Cookie

interface CaptchaService {
    fun downloadCaptcha(url: String, cookies: List<Cookie>, data: (captchaPath: String, newPHPSESSIDCookie: Cookie) -> Unit)
    fun solveCaptcha(fileName: String): String
}