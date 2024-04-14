package org.example.classes_feature.data

import kotlinx.serialization.Serializable

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
        return ClassObject(
            id = this.id,
            title = this.title,
            color = this.color,
            start = this.start,
            end = this.end,
            description = description
        )
    }
}