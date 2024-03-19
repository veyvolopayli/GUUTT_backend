package org.example.authorization_feature.guu

import java.io.File

open class GuuCaptchaServiceImpl: GuuCaptchaService {
    override fun solveCaptcha(fileName: String): String {
        val processBuilder = ProcessBuilder("py/venv/Scripts/python.exe", "py/captcha_solver.py", fileName)
        val process = processBuilder.start()

        // Ожидаем завершения работы скрипта и получаем exitCode
        val exitCode = process.waitFor()
        if (exitCode != 0) return ""


        // После выхода из программы мы имеем .txt файл с решением капчи
        val txtFileName = fileName.replace(".png", ".txt")
        val captchaTxtFile = File("py/$txtFileName")
        val captcha = captchaTxtFile.readText().trim()
        if (captchaTxtFile.exists()) captchaTxtFile.delete()

        val imageFile = File("py/$fileName")
        if (imageFile.exists()) imageFile.delete()

        return captcha
    }

}