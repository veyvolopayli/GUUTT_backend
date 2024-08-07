package org.guutt.api.authorization

interface AuthorizationService {
    suspend fun setupUserWithCommonData(login: String, password: String, alreadyRegistered: Boolean): AuthResult
}