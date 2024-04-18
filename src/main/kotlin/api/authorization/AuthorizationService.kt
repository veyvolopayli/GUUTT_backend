package org.example.api.authorization

interface AuthorizationService {
    suspend fun authenticate(login: String, password: String): AuthResult
}