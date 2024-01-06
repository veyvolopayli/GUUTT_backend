package org.example

import com.google.gson.Gson
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.Form
import org.http4k.core.body.form
import org.http4k.core.body.formAsMap
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer

fun main() {
    val books = listOf("Book 1", "Book 2", "Book 3")

    val api = routes(
        "books" bind Method.GET to { _: Request ->
            Response(OK).body(books.toString())
        },
        "books/{n}" bind  Method.GET to {r: Request ->
            r.path("n")?.toIntOrNull()?.let {
                val book = books.getOrNull(it - 1)
                if (book != null) Response(OK).body(book)
                else Response(BAD_REQUEST).body("Book not found!")
            } ?: Response(NOT_FOUND).body("Book number required")
        }
    )

    api.asServer(Undertow(9000)).start()

    val client = ApacheClient()

    val request = Request(method = Method.GET, uri = "https://my.guu.ru/student/classes")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Cookie", "PHPSESSID=lk0b9osbo0ndlshuenhqcg7irm686iab; _csrf=61d1872a08c0f8a6b702d6eb7935135ff0bb3ae7aeb5b9ba3b5d2a4be9bc9b54a%3A2%3A%7Bi%3A0%3Bs%3A5%3A%22_csrf%22%3Bi%3A1%3Bs%3A32%3A%225fjECubmYzLv5T7wAkteSUQIu59uoqxj%22%3B%7D; modal_mess=f3c8bfc267bb4b94c083aae3b0d485801d05b01962e52bbb1a3b514cd6fa1542a%3A2%3A%7Bi%3A0%3Bs%3A10%3A%22modal_mess%22%3Bi%3A1%3Bs%3A10%3A%222024-01-06%22%3B%7D; session-cookie=17a799644f8077afb664fc6d18991a2485ae05abe53d04fe03a2fcbf7f30c4ff280a7a2fb2f8eb122c8b894b5a511427")

    val response = client(request)
    println(response)
}