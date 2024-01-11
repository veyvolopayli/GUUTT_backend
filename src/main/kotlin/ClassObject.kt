package org.example

import java.time.LocalDateTime

data class ClassObject(
    val id: Long,
    val title: String,
    val color: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val description: ClassDescription
)
