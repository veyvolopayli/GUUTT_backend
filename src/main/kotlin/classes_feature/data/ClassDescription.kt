package org.example.classes_feature.data

import kotlinx.serialization.Serializable

@Serializable
data class ClassDescription(
    val building: String,
    val classroom: String,
    val event: String,
    val professor: String,
    val department: String
)
