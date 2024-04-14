package org.example.api.authorization.deprecated

interface GuuCaptchaService {
    fun solveCaptcha(fileName: String): String
}