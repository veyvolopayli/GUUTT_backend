package org.example

fun org.http4k.core.Request.applyHeaders(cookie: String): org.http4k.core.Request {
    return this.header("Accept", Headers.ACCEPT_HEADER)
        .header("User-Agent", Headers.USER_AGENT_HEADER)
        .header("Connection", "keep-alive")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Cookie", cookie)
}

fun org.http4k.core.Response.withHeaders(): org.http4k.core.Response {
    return this.header("Content-Type", "application/json; charset=utf-8")
}

fun Map<String, List<ClassObject>>.fillDatesGaps(): Map<String, List<ClassObject>> {
    val thisYear = java.time.LocalDate.now().year

    // Написать более оптимизированный алгоритм
    val siftedDaysMap = this.filter {
        it.key.take(4).toInt() >= thisYear - 1
    }

    val startDate = siftedDaysMap.keys.first()
    val endDate = siftedDaysMap.keys.last()

    val filledDateMap = mutableMapOf<java.time.LocalDate, List<ClassObject>>()
    val format = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val sDate = java.time.LocalDate.parse(startDate, format)
    val eDate = java.time.LocalDate.parse(endDate, format)

    var currentDate = sDate

    while (!currentDate.isAfter(eDate)) {
        filledDateMap[currentDate] = siftedDaysMap.getOrDefault(currentDate.toString(), emptyList())
        currentDate = currentDate.plus(1, java.time.temporal.ChronoUnit.DAYS)
    }

    return filledDateMap.map {
        it.key.toString() to it.value
    }.toMap()
}