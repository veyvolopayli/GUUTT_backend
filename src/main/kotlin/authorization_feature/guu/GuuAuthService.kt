package org.example.authorization_feature.guu

interface GuuAuthService {
    fun downloadCaptcha(
        data: (fileName: String, cookies: List<org.http4k.core.cookie.Cookie>) -> Unit
    )
    fun authorize(login: String, password: String)
}