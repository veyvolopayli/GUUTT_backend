package org.example

interface GuuWebsiteService {
    fun fetchGroup(cookie: String): String?
    fun fetchClasses(cookie: String): GuuResponse<List<ClassObject>>
    fun fetchNews(): List<News>
    fun fetchFullName(cookie: String): GuuResponse<String>
    fun getUserInfo(cookie: String): GuuResponse<UserInfo>
}