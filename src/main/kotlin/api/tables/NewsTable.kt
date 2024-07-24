package org.guutt.tables

import org.guutt.tables.response.DbResponse
import org.guutt.News
import org.guutt.NewsDetailed
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

object NewsTable : Table("news") {
    private val imageUrl = varchar("image_url", 255)
    private val title = varchar("title", 255)
    private val description = varchar("description", 500)
    private val date = varchar("date", 50)
    private val href = varchar("href", 255)
    private val fullBody = varchar("full_body", 65535)

    fun fetchAllNews(): DbResponse<List<News>> {
        return try {
            transaction {
                val news = select(imageUrl, title, description, date, href).map {
                    News(
                        imageUrl = it[imageUrl],
                        title = it[title],
                        description = it[description],
                        date = it[date],
                        href = it[href]
                    )
                }
                DbResponse.Success(news)
            }
        } catch (e: Exception) {
            println(e.message)
            DbResponse.Error(e.message.toString())
        }
    }

    fun fetchNewsByTitle(title: String): DbResponse<NewsDetailed> {
        return try {
            transaction {
                val singleNews = select(imageUrl, fullBody, date, href).where { NewsTable.title.eq(title) }.map {
                    NewsDetailed(
                        imageUrl = it[imageUrl],
                        title = title,
                        body = it[fullBody],
                        date = it[date],
                        href = it[href]
                    )
                }.first()
                DbResponse.Success(singleNews)
            }
        } catch (e: Exception) {
            println(e.message)
            DbResponse.Error(e.message.toString())
        }
    }

    fun insertNews(news: List<News>): DbResponse<Unit> {
        return try {
            transaction {
                batchInsert(news, true) {
                    this[imageUrl] = it.imageUrl
                    this[title] = it.title
                    this[description] = it.description
                    this[date] = it.date
                    this[href] = it.href
                    this[fullBody] = it.description
                }
                DbResponse.Success(Unit)
            }
        } catch (e: Exception) {
            println(e.message)
            DbResponse.Error(e.message.toString())
        }
    }
}