package org.guutt.tesseract

import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import java.awt.image.BufferedImage
import java.io.File

open class CaptchaServiceImpl(private val tesseract: Tesseract): CaptchaService {
    override suspend fun solveAndDelete(file: File): String {
        return try {
            tesseract.doOCR(file).lowercase()
        } catch (e: TesseractException) {
            e.printStackTrace()
            ""
        } finally {
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override suspend fun solveAndDelete(filePath: String): String {
        val file = File(filePath)
        return try {
            tesseract.doOCR(File(filePath))
        } catch (e: TesseractException) {
            e.printStackTrace()
            ""
        } finally {
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override suspend fun solve(bufferedImage: BufferedImage): String? {
        return try {
            tesseract.doOCR(bufferedImage)
        } catch (e: TesseractException) {
            null
        }
    }
}