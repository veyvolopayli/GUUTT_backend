package org.example.tables

import org.example.tables.response.DbResponse
import org.example.UserInfo
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction

object UserDataTable : Table("user_data") {
    private val loginColumn = varchar("login", 50).uniqueIndex()
    private val fullNameColumn = varchar("full_name", 100)
    private val groupColumn = varchar("group", 100)
    private val registerTimeColumn = long("register_date")

    fun insertData(login: String, userInfo: UserInfo): DbResponse<Unit> {
        return try {
            transaction {
                insertIgnore {
                    it[loginColumn] = login
                    it[fullNameColumn] = userInfo.fullName
                    it[groupColumn] = userInfo.group
                    it[registerTimeColumn] = System.currentTimeMillis()
                }
                DbResponse.Success(Unit)
            }
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }

    fun getAllUsersLoginsGroups(): DbResponse<List<UserLoginGroup>> {
        return try {
            transaction {
                DbResponse.Success(select(loginColumn, groupColumn).map {
                    UserLoginGroup(
                        login = it[loginColumn],
                        group = it[groupColumn]
                    )
                })
            }
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }

    data class UserLoginGroup(val login: String, val group: String)
}