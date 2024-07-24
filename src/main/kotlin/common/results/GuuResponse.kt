package org.guutt.common.results

sealed class GuuResponse<T> {
    class Success<T>(val data: T): GuuResponse<T>()
    class CookieExpired<T>: GuuResponse<T>()
    class NotResponding<T>(val reason: String): GuuResponse<T>()
    class Forbidden<T>: GuuResponse<T>()
}