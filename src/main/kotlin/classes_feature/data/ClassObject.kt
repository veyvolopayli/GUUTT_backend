package org.example.classes_feature.data

import kotlinx.serialization.Serializable
import org.example.classes_feature.data.ClassDescription

@Serializable
data class ClassObject(
    val id: Long,
    val title: String,
    val color: String,
    val start: String,
    val end: String,
    val description: ClassDescription
)
