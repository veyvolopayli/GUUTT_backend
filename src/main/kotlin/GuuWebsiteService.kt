package org.example

import api.tables.UserDetails
import org.example.classes_feature.data.ClassObject
import org.example.common.results.GuuResponse

interface GuuWebsiteService {
    fun fetchClasses(cookies: String): GuuResponse<List<ClassObject>?>
    fun fetchNews(): List<News>
    fun fetchUserDetails(cookies: String): GuuResponse<UserDetails>
}