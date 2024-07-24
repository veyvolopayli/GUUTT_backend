package org.guutt

import kotlinx.serialization.Serializable

@Serializable
data class News(
    val imageUrl: String,
    val title: String,
    val description: String,
    val date: String,
    val href: String
)
