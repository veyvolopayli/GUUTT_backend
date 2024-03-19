package org.example.authorization_feature.guu

interface GuuAuthService {
    fun procesAuth(login: String, password: String): AuthResult
}