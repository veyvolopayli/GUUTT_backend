package org.example

import kotlinx.serialization.json.Json
import org.example.tables.NewsTable
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GuuWebsiteServiceImpl(private val client: HttpHandler) : GuuWebsiteService {
    override fun fetchGroup(cookie: String): String? {
        val request = Request(method = Method.GET, uri = GuuLinks.STUDENT)
            .applyHeaders(cookie)
        val response = client(request)
        val group = parseGroup(response.body.toString())
        return group
    }

    override fun fetchClasses(cookie: String): GuuResponse<List<ClassObject>> {
        val request = Request(method = Method.GET, uri = GuuLinks.CLASSES)
            .applyHeaders("text/html; charset=UTF-8")
            .cookieString(cookie)
        // if response code 302 - cookies isn't valid, else if 200 - all is good
        val response = client(request)
        return when(response.status.code) {
            200 -> {
                val classes = parseClasses(response.bodyString())
                val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                val sortedClasses = classes.sortedBy {
                    LocalDateTime.parse(it.start, dateTimeFormat)
                }
                GuuResponse.Success(sortedClasses)
                // all is ok
            }
            403 -> {
                GuuResponse.Forbidden()
            }
            302 -> {
                // cookie is bad
                GuuResponse.CookieExpired()
            }
            else -> {
                GuuResponse.NotResponding("${response.status.code}: ${response.status.description}")
            }
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

    override fun fetchFullName(cookie: String): GuuResponse<String> {
        val request = Request(method = Method.GET, uri = GuuLinks.STUDENT)
            .applyHeaders("text/html; charset=UTF-8")
            .cookieString(cookie)
        val response = ApacheClient().invoke(request)
        return when(response.status.code) {
            200 -> {
                val doc = Jsoup.parse(response.bodyString())
                val userFullName = doc.select("h3.widget-user-username").first()?.text() ?: ""
                GuuResponse.Success(userFullName)
            }
            302 -> {
                GuuResponse.CookieExpired()
            }
            403 -> {
                GuuResponse.Forbidden()
            }
            else -> {
                GuuResponse.NotResponding("${response.status.code} ${response.status.description}")
            }
        }
    }

    override fun getUserInfo(cookie: String): GuuResponse<UserInfo> {
        val request = Request(Method.GET, GuuLinks.STUDENT)
            .applyHeaders("text/html; charset=UTF-8")
            .cookieString(cookie)
        val response = ApacheClient().invoke(request)
        return when(response.status.code) {
            200 -> {
                val doc = Jsoup.parse(response.bodyString())
                val userFullName = doc.select("h3.widget-user-username").first()?.text() ?: ""
                val userGroup = doc.select("h5.widget-user-desc").first()?.text() ?: ""
                GuuResponse.Success(UserInfo(fullName = userFullName, group = userGroup))
            }
            302 -> {
                GuuResponse.CookieExpired()
            }
            403 -> {
                GuuResponse.Forbidden()
            }
            else -> {
                GuuResponse.NotResponding("${response.status.code} ${response.status.description}")
            }
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
                    val classDescription = parseClassDescription(it.description)
                    it.toClassObject(classDescription)
                }
            }
        }
        return emptyList()
    }

    private fun parseClassDescription(htmlDescription: String): ClassDescription {

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