package org.example.tables

import org.example.DbResponse
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object UserDataTable : Table("user_data") {
    private val loginColumn = varchar("login", 50).uniqueIndex()
    private val fullNameColumn = varchar("full_name", 100)
    private val groupColumn = varchar("group", 100)
    private val registerTimeColumn = long("register_date")

    fun saveData(login: String, fullName: String, group: String): DbResponse<Unit> {
        return try {
            transaction {
                insert {
                    it[loginColumn] = login
                    it[fullNameColumn] = fullName
                    it[groupColumn] = group
                    it[registerTimeColumn] = System.currentTimeMillis()
                }
                DbResponse.Success(Unit)
            }
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }

    fun getAllGroups(): DbResponse<List<String>> {
        return try {
            transaction {
                DbResponse.Success(select(groupColumn).map { it[groupColumn] })
            }
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }
    
}