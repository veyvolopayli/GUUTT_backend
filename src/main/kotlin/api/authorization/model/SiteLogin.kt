package api.authorization.model

import kotlinx.serialization.Serializable

@Serializable
data class SiteLogin(
    val captcha: String,
    val login: String,
    val password: String
)