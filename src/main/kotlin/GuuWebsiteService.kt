package org.example

import org.example.classes_feature.data.ClassObject
import org.example.common.results.GuuResponse

interface GuuWebsiteService {
    fun fetchGroup(cookie: String): String?
    fun fetchClasses(cookie: String): GuuResponse<List<ClassObject>>
    fun fetchNews(): List<News>
    fun fetchFullName(cookie: String): GuuResponse<String>
    fun getUserInfo(cookie: String): GuuResponse<UserInfo>
}