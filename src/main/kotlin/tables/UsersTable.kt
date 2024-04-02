package org.example.tables

import org.example.DbResponse
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.postgresql.util.PSQLException

object UsersTable: Table("users") {
    private val loginColumn = varchar("login", 50).uniqueIndex()
    private val passwordColumn = varchar("password", 24)
    private val cookiesColumn = varchar("cookies", 255)

    fun insertData(login: String, password: String, cookies: String): DbResponse<Unit> {
        return try {
            transaction {
                insert {
                    it[loginColumn] = login
                    it[passwordColumn] = password
                    it[cookiesColumn] = cookies
                }
                DbResponse.Success(Unit)
            }
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }

    fun checkUserExistence(login: String): Boolean? {
        return try {
            transaction {
                select(loginColumn).where {
                    loginColumn eq login
                }.count() > 0
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getPassword(login: String): DbResponse<String> {
        return try {
            transaction {
                val password = select(passwordColumn).where {
                    loginColumn eq login
                }.first()[passwordColumn]
                DbResponse.Success(password)
            }
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }

    fun getAllLogins(): DbResponse<List<String>> {
        return try {
            transaction {
                val logins = select(loginColumn).map { it[loginColumn] }
                DbResponse.Success(logins)
            }
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }

    fun getCookies(login: String): DbResponse<String> {
        return try {
            transaction {
                val cookies = select(cookiesColumn).where { loginColumn eq login }.single()[cookiesColumn]
                DbResponse.Success(cookies)
            }
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }

    fun getAllAdminsCookies(): List<AdminCookies> {
        return try {
            transaction {
                select(loginColumn, cookiesColumn).map {
                    AdminCookies(
                        login = it[loginColumn],
                        cookies = it[cookiesColumn]
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    data class AdminCookies(val login: String, val cookies: String)

    fun updateCookies(login: String, newCookies: String): Boolean? {
        return try {
            transaction {
                update({ loginColumn eq login }) {
                    it[cookiesColumn] = newCookies
                } == 1
            }
        } catch (e: Exception) {
//            e.printStackTrace()
            null
        }
    }
}