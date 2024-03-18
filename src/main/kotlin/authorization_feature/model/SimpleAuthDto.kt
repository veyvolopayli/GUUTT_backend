package org.example.authorization_feature.model

import authorization_feature.model.SiteLogin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimpleAuthDto(
    @SerialName("SiteLogin") val siteLogin: SiteLogin
)