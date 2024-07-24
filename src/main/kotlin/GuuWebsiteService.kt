package org.guutt

import api.tables.UserDetails
import org.guutt.classes_feature.data.ClassObject
import org.guutt.common.results.GuuResponse

interface GuuWebsiteService {
    fun fetchClasses(cookies: String): GuuResponse<List<ClassObject>?>
    fun fetchNews(): List<News>
    fun fetchUserDetails(cookies: String): GuuResponse<UserDetails>
}