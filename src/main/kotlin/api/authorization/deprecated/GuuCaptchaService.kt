package org.guutt.api.authorization.deprecated

interface GuuCaptchaService {
    fun solveCaptcha(fileName: String): String
}