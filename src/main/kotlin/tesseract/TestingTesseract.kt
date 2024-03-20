package org.example.tesseract

import net.sourceforge.tess4j.Tesseract
import org.example.GuuLinks
import org.example.applyHeaders
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import java.io.File

class TestingTesseract {
    private val tesseract = Tesseract().also { it.setDatapath("src/main/kotlin/tesseract/tessdata") }
    private val imagesDir = File("src/main/kotlin/tesseract/images")
    private val imagesAfterTuningDir = File("src/main/kotlin/tesseract/images_after_fine_tuning")
    fun downloadAndSolveCaptchaImages(count: Int) {
        repeat(count) { n ->
            val fileNum = String.format("%03d", n)
            val fileName = "image-$fileNum.png"
            val captchaFile = downloadCaptcha(fileName)
            solveCaptchaToFile(captchaFile)
        }
    }

    private fun downloadCaptcha(fileName: String): File? {
        return try {
            val request = Request(Method.GET, GuuLinks.CAPTCHA).applyHeaders("image/png")
            val response = ApacheClient().invoke(request)
            val stream = response.body.stream

            File(imagesAfterTuningDir, fileName).apply {
                println(createNewFile())
                writeBytes(stream.readAllBytes())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun solveCaptchaToFile(file: File?): File? {
        file ?: return null
        return try {
            val result = tesseract.doOCR(file)
            File(imagesAfterTuningDir, file.name.replace(".png", ".gt.txt")).apply {
                createNewFile()
                writeText(result.lowercase())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renameTxtFiles() {
        imagesDir.listFiles()?.forEach {
            println(it.path)
            val newFileName = it.name.replace("image", "screenshot")
            val newFile = File(imagesDir, newFileName)
            it.renameTo(newFile)
        }
    }

    fun testOcr() {
        imagesDir.listFiles()?.onEach {
            if (it.name.contains(".png")) {
                solveCaptchaToFile(it)
            }
        }
    }

}

fun main() {
    val t = TestingTesseract()
    t.downloadAndSolveCaptchaImages(30)
//    t.renameTxtFiles()
}