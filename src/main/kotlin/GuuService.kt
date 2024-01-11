package org.example

interface GuuService {
    fun fetchGroup(cookie: String): String?
    fun fetchClasses(cookie: String): List<ClassObject>
}