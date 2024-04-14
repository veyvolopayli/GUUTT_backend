package api.authorization.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimpleAuthDto(
    @SerialName("SiteLogin") val siteLogin: SiteLogin
)