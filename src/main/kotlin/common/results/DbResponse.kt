package org.example.tables.response

sealed class DbResponse<T> {
    class Success<T>(val data: T): DbResponse<T>()
    class Error<T>(val message: String) : DbResponse<T>()
}