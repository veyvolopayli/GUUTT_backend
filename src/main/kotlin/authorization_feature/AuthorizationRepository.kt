package org.example.authorization_feature

import org.http4k.core.Response

interface AuthorizationRepository {
    fun getAuthPage(data: (htmlResponse: Response, captchaName: String) -> Unit)
    fun authorize(login: String, password: String)
    fun recognizeCaptcha(fileName: String): String
    fun String.extractCaptchaImageUrl(): String
}