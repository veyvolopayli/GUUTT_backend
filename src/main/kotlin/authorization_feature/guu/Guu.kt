package org.example.authorization_feature.guu

import org.http4k.core.Response

interface Guu {
    fun getClassesPage(cookies: String): Response
    fun getProfilePage(cookies: String): Response
}