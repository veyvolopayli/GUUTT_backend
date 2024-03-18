package authorization_feature.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthDto(
    @SerialName("SiteLogin") val siteLogin: SiteLogin,
    @SerialName("_csrf") val csrf: String,
    @SerialName("csrftoken") val csrfToken: String,
    @SerialName("login-button") val loginButton: String = ""
)