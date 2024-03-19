package org.example

object GuuLinks {
    const val CAPTCHA = "https://my.guu.ru/site/captcha"
    const val STUDENT = "https://my.guu.ru/student"
    const val CLASSES = "https://my.guu.ru/student/classes"
    const val AUTH = "https://my.guu.ru/auth/login"
}

object Headers {
    const val ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
    const val USER_AGENT_HEADER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
}

object GuuAuthMessages {
    const val INCORRECT_LOGIN_PASSWORD = "Проверьте правильность введенных данных"
    const val INCORRECT_CAPTCHA = "Неправильный проверочный код"
}