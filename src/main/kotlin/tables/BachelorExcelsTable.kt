package org.example.tables

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

object BachelorExcelsTable : Table("bachelor_excel") {
    private val courseColumn = integer("course").uniqueIndex()
    private val hashColumn = varchar("hash", 44).uniqueIndex()
    private val timestampColumn = long("timestamp")

    fun getHashes(): Map<Int, String> {
        return try {
            transaction {
                select(courseColumn, hashColumn).associate {
                    it[courseColumn] to it[hashColumn]
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun updateHash(course: Int, hash: String) {
        try {
            transaction {
                update({ courseColumn eq course }) {
                    it[hashColumn] = hash
                    it[timestampColumn] = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun insertHashes(hashesMap: Map<Int, String>): Boolean {
        return try {
            transaction {
                batchInsert(hashesMap.entries) {
                    this[courseColumn] = it.key
                    this[hashColumn] = it.value
                    this[timestampColumn] = System.currentTimeMillis()
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun insertHash(course: Int, hash: String): Boolean {
        return try {
            transaction {
                insert {
                    it[courseColumn] = course
                    it[hashColumn] = hash
                    it[timestampColumn] = System.currentTimeMillis()
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

fun main() {
    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD")
    )
    val startDate = LocalDate.of(2024, 3, 30)
    val endDate = LocalDate.of(2024, 5, 11)
    println(ClassesTable.test(startDate to endDate))
}