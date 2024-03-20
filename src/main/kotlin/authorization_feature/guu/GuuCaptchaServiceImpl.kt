package org.example.authorization_feature.guu

import org.example.captchaTempStorageDir
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

open class GuuCaptchaServiceImpl: GuuCaptchaService {
    override fun solveCaptcha(fileName: String): String {
        try {
            val isOsLinux = System.getProperty("os.name") == "Linux"
            val python = if (isOsLinux) {
                "/root/guutt/python/env/bin/python"
            } else {
                "py/venv/Scripts/python.exe"
            }

            val pySolver = if (isOsLinux) {
                "/root/guutt/python/captcha_solver.py"
            } else {
                "py/captcha_solver.py"
            }

            val processBuilder = ProcessBuilder(python, pySolver, fileName)
            val process = processBuilder.start()

            val errorStream = process.errorStream

            val error = BufferedReader(InputStreamReader(errorStream)).readText()

            val f = File("py", "err.txt")
            f.apply {
                createNewFile()
                writeText(error)
            }

            // Ожидаем завершения работы скрипта и получаем exitCode
            val exitCode = process.waitFor()

            println(exitCode)

            if (exitCode != 0) return ""

            // После выхода из программы мы имеем .txt файл с решением капчи
            val txtFileName = fileName.replace(".png", ".txt")
            val captchaTxtFile = File(captchaTempStorageDir, txtFileName)
            val captcha = captchaTxtFile.readText().trim()
//            if (captchaTxtFile.exists()) captchaTxtFile.delete()

            return captcha
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        } finally {
            val imageFile = File(captchaTempStorageDir, fileName)
//            if (imageFile.exists()) imageFile.delete()
        }
    }

}