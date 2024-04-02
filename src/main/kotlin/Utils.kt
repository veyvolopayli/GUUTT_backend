package org.example

import java.time.LocalDate

fun currentSemester(): Pair<LocalDate, LocalDate>? {
    val currentDate = LocalDate.now()
    // Год, месяц, день
    val summerSemesterStart = LocalDate.of(currentDate.year, 9, 1)
    val summerSemesterEnd = LocalDate.of(currentDate.year, 12, 31)
    val winterSemesterStart = LocalDate.of(currentDate.year, 1, 1)
    val winterSemesterEnd = LocalDate.of(currentDate.year, 6, 30)

    return when (currentDate) {
        in summerSemesterStart..summerSemesterEnd -> summerSemesterStart to summerSemesterEnd
        in winterSemesterStart..winterSemesterEnd -> winterSemesterStart to winterSemesterEnd
        else -> null
    }
}