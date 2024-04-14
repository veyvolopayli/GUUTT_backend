package org.example.tesseract

import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import java.io.File

open class CaptchaServiceImpl(private val tesseract: Tesseract): CaptchaService {
    override fun solveAndDelete(file: File): String {
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

    override fun solveAndDelete(filePath: String): String {
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
}