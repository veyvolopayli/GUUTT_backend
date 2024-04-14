package org.example.common

import org.example.classes_feature.data.ClassObject
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies

fun org.http4k.core.Request.applyHeaders(contentType: String): org.http4k.core.Request {
    return this.header("Accept", Headers.ACCEPT_HEADER)
        .header("User-Agent", Headers.USER_AGENT_HEADER)
        .header("Connection", "keep-alive")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Content-Type", contentType)
}

fun org.http4k.core.Request.cookieString(value: String): org.http4k.core.Request {
    return this.header("Cookie", value)
}

fun org.http4k.core.Response.cookiesString(): String {
    return this.cookies().joinToString(";") {
        "${it.name}=${it.value}"
    }
}

fun List<org.http4k.core.cookie.Cookie>.stringify(): String {
    return joinToString(";") {
        "${it.name}=${it.value}"
    }
}

fun org.http4k.core.Response.specifyContentType(): org.http4k.core.Response {
    return this.header("Content-Type", "application/json; charset=utf-8")
}

fun org.http4k.core.Request.applyAuthCookies(cookies: List<org.http4k.core.cookie.Cookie>): org.http4k.core.Request {
    return cookies.fold(this) { request, cookie ->
        request.cookie(cookie)
    }
}

fun Map<String, List<ClassObject>>.fillDatesGaps(): Map<String, List<ClassObject>> {
    val startDate = this.keys.first()
    val endDate = this.keys.last()

    val filledDateMap = mutableMapOf<java.time.LocalDate, List<ClassObject>>()
    val format = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val sDate = java.time.LocalDate.parse(startDate, format)
    val eDate = java.time.LocalDate.parse(endDate, format)

    var currentDate = sDate

    while (!currentDate.isAfter(eDate)) {
        filledDateMap[currentDate] = this.getOrDefault(currentDate.toString(), emptyList())
        currentDate = currentDate.plus(1, java.time.temporal.ChronoUnit.DAYS)
    }

    return filledDateMap.map {
        it.key.toString() to it.value
    }.toMap()
}