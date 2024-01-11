package org.example

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database

fun main() {

    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD")
    )

    val client = ApacheClient()
    val guuService = GuuServiceImpl(client)

    val api = routes(
        "classes" bind Method.GET to { r: Request ->
            r.header("g")?.let { group ->
                // todo SELECT DATA FROM DB
                Response(OK).body("LIST")
            } ?: Response(BAD_REQUEST).body("Group required")
        },
        "authorize" bind Method.GET to { r: Request ->
            r.header("c")?.let { cookie ->
                // todo REQUEST TO GUU. FETCH GROUP AND CLASSES. SAVE TO DB
                val group = guuService.fetchGroup(cookie = cookie) ?: return@let Response(CONFLICT).body("GUU server side error...")

                val classes = guuService.fetchClasses(cookie = cookie)

                val isInserted = ClassesTable.insertClasses(studentGroup = group, classObjects = classes) ?: return@let Response(CONFLICT).body("База калла")

                Response(OK).body(isInserted.toString())
            } ?: Response(BAD_REQUEST).body("Cookie required")
        }
    )

    api.asServer(Undertow(9000)).start()
}