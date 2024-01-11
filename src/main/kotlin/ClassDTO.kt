package org.example

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class ClassDTO(
    val id: Long,
    val title: String,
    val color: String,
    val start: String,
    val end: String,
    val description: String
) {
    fun toClassObject(description: ClassDescription): ClassObject {
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val startDate = LocalDateTime.parse(this.start, format)
        val endDate = LocalDateTime.parse(this.end, format)

        return ClassObject(
            id = this.id,
            title = this.title,
            color = this.color,
            start = startDate,
            end = endDate,
            description = description
        )
    }
}