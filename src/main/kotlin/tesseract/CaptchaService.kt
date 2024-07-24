package org.guutt.tesseract

import java.awt.image.BufferedImage
import java.io.File

interface CaptchaService {
    suspend fun solveAndDelete(file: File): String
    suspend fun solveAndDelete(filePath: String): String
    suspend fun solve(bufferedImage: BufferedImage): String?
}