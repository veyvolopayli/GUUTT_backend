package org.example.api.authorization

interface AuthorizationService {
    fun authenticate(login: String, password: String): AuthResult
}