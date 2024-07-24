package org.guutt.api.authorization

import org.http4k.core.Response

interface Guu {
    fun getClassesPage(cookies: String): Response
    fun getProfilePage(cookies: String): Response
}