package org.example

import kotlinx.serialization.Serializable

@Serializable
data class NewsDetailed(
    val imageUrl: String,
    val title: String,
    val body: String,
    val date: String,
    val href: String
)
