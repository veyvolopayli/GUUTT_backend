package org.example.authorization_feature.guu

sealed class GuuAuthErrorType {
    data object WrongLoginOrPassword: GuuAuthErrorType()
    data object WrongCaptcha: GuuAuthErrorType()
    data object UnknownError: GuuAuthErrorType()
    data object UnexpectedError: GuuAuthErrorType()
}