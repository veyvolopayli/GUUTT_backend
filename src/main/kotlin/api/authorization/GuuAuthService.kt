package org.guutt.api.authorization

import org.http4k.core.cookie.Cookie
import java.awt.image.BufferedImage
import java.io.File

interface GuuAuthService {
    suspend fun authorize(login: String, password: String, captcha: String, cookies: String): GuuWebsiteAuthResult
    suspend fun downloadCaptcha(): Pair<File, List<Cookie>>?
    fun getCaptcha(): Pair<BufferedImage, List<Cookie>>?
}