package org.example.tesseract

import java.io.File

interface CaptchaService {
    fun solveAndDelete(file: File): String
    fun solveAndDelete(filePath: String): String
}