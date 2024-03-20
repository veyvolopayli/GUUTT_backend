package org.example.tesseract

import java.io.File

interface CaptchaService {
    fun solveCaptcha(file: File): String
    fun solveCaptcha(filePath: String): String
}