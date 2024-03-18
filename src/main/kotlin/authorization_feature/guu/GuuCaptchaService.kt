package org.example.authorization_feature.guu

interface GuuCaptchaService {
    fun solveCaptcha(fileName: String): String
}