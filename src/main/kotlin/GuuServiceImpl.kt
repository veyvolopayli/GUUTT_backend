package org.example

import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.serialization.json.Json
import org.http4k.core.*
import org.jsoup.Jsoup

class GuuServiceImpl(private val client: HttpHandler) : GuuService {
    override fun fetchGroup(cookie: String): String? {
        val request = Request(method = Method.GET, uri = GuuLinks.STUDENT)
            .header("Accept", GuuLinks.ACCEPT_HEADER)
            .header("User-Agent", GuuLinks.USER_AGENT_HEADER)
            .header("Connection", "keep-alive")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Cookie", cookie)
        val response = client(request)
        val group = parseGroup(response.body.toString())
        return group
    }

    override fun fetchClasses(cookie: String): List<ClassObject> {
        val request = Request(method = Method.GET, uri = GuuLinks.CLASSES)
            .header("Accept", GuuLinks.ACCEPT_HEADER)
            .header("User-Agent", GuuLinks.USER_AGENT_HEADER)
            .header("Connection", "keep-alive")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Cookie", cookie)
        val response = client(request)
        return parseClasses(response.body.toString()).sortedBy { it.start }
    }

    private fun parseGroup(studentPage: String): String? {
        val document = Jsoup.parse(studentPage)
        val group = document.select("h5.widget-user-desc").first()?.text()
        return group
    }

    private fun parseClasses(classesPage: String): List<ClassObject> {
        val document = Jsoup.parse(classesPage)
        val scriptElements = document.select("script")

        for (script in scriptElements) {
            val scriptContent: String = script.data()

            if (scriptContent.contains("fullCalendar")) {
                val startIndex = scriptContent.indexOf("events : ") + "events : ".length
                val endIndex = scriptContent.indexOfLast { it == ']' } + 1
                val eventsJsonArray = scriptContent.substring(startIndex, endIndex)

                val json = Json { ignoreUnknownKeys = true }

                val classDTOs = json.decodeFromString<List<ClassDTO>>(eventsJsonArray)

                return classDTOs.map {
                    val classDescription = parseDescription(it.description)
                    println(classDescription)
                    it.toClassObject(classDescription)
                }
            }
        }
        return emptyList()
    }

    private fun parseDescription(htmlDescription: String): ClassDescription {

        val sDescription = htmlDescription.split("<br>")
        val map = mutableMapOf<String, String>()
        sDescription.forEach {
            if (it.isNotEmpty()) {
                val key = it.substringAfter("<b>").substringBefore("</b>")
                val value = it.substringAfter("</b> ")
                map[key] = value
            }
        }

        val building = map.getOrDefault("Здание:", "")
        val classroom = map.getOrDefault("Аудитория:", "")
        val event = map.getOrDefault("Событие:", "")
        val professor = map.getOrDefault("Преподаватель:", "")
        val department = map.getOrDefault("Кафедра:", "")

        return ClassDescription(building, classroom, event, professor, department)
    }

}