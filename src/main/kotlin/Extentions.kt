package org.example

fun org.http4k.core.Request.applyHeaders(cookie: String): org.http4k.core.Request {
    return this.header("Accept", Headers.ACCEPT_HEADER)
        .header("User-Agent", Headers.USER_AGENT_HEADER)
        .header("Connection", "keep-alive")
        .header("Accept-Encoding", "gzip, deflate, br")
        .header("Cookie", cookie)
}

fun org.http4k.core.Response.withHeaders(): org.http4k.core.Response {
    return this.header("Content-Type", "application/json; charset=utf-8")
}