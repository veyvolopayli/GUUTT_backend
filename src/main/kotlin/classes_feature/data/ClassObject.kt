package org.guutt.classes_feature.data

import kotlinx.serialization.Serializable

@Serializable
data class ClassObject(
    val id: Long,
    val title: String,
    val color: String,
    val start: String,
    val end: String,
    val description: ClassDescription,
    val group: String? = null
)
