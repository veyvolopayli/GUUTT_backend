package org.example.api.authorization

import org.http4k.core.cookie.Cookie
import java.io.File

interface GuuAuthService {
    fun authorize(login: String, password: String, captcha: String, cookies: String): GuuWebsiteAuthResult
    fun downloadCaptcha(): Pair<File, List<Cookie>>?
}