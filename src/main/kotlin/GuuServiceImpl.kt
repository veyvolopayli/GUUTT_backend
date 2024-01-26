package org.example

import kotlinx.serialization.json.Json
import org.example.tables.NewsTable
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GuuServiceImpl(private val client: HttpHandler) : GuuService {
    override fun fetchGroup(cookie: String): String? {
        val request = Request(method = Method.GET, uri = GuuLinks.STUDENT)
            .applyHeaders(cookie)
        val response = client(request)
        val group = parseGroup(response.body.toString())
        return group
    }

    override fun fetchClasses(cookie: String): List<ClassObject> {
        val request = Request(method = Method.GET, uri = GuuLinks.CLASSES)
            .applyHeaders(cookie)
        val response = client(request)
        return parseClasses(response.body.toString()).sortedBy {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime.parse(it.start, format)
        }
    }

    override fun fetchNews(): List<News> {
        return try {
            val newsPages = 10
            val newsList = mutableListOf<News>()
            for (page in 1..newsPages) {
                val document: Document = Jsoup.connect("https://guu.ru/category/news_ru/page/$page/").get()
                val news = parseNews(document)
                newsList.addAll(news)
            }
            NewsTable.insertNews(newsList)
            newsList
        } catch (e: Exception) {
            println(e.message)
            return emptyList()
        }
    }

    private fun parseNews(document: Document): List<News> {
        val imageElements = document.getElementsByClass("img-holder thumbnail pull-left")
        val elements = document.getElementsByClass("post cf")
        val news = elements.mapIndexed { i, element ->
            val imageUrl = imageElements[i].select("img").first()?.absUrl("src").toString()
            val title = element.select("h3").first()?.text().toString()
            val description = element.select("p").eachText().joinToString("\n")
            val date = element.select("small").find {
                !it.text().toString().contains("Анонсы") && !it.text().toString().contains("Новости")
            }?.text().toString()
            val href = element.select("a").first()?.attr("href").toString()
            News(imageUrl, title, description,  date, href)
        }

        return news
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