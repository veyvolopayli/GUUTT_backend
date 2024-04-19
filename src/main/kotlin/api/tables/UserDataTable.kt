package org.example.tables

import api.authorization.security.AesEncryption
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.sourceforge.tess4j.Tesseract
import org.example.tables.response.DbResponse
import org.example.UserInfo
import org.example.api.authorization.AuthResult
import org.example.api.authorization.AuthorizationServiceImpl
import org.example.api.authorization.GuuAuthServiceImpl
import org.example.common.tessdataDir
import org.example.logger
import org.example.tesseract.CaptchaServiceImpl
import org.jetbrains.exposed.sql.*
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

    fun selectAllWithCookies(aesEncryption: AesEncryption): List<UserLoginGroup>? {
        return try {
            transaction {
                join(
                    otherTable = UsersTable,
                    joinType = JoinType.INNER,
                    onColumn = loginColumn,
                    otherColumn = UsersTable.loginColumn
                ).select(loginColumn, groupColumn, UsersTable.cookiesColumn)
                    .map {
                        val login = it[loginColumn]
                        val group = it[groupColumn]
                        val cookies = it[UsersTable.cookiesColumn]
                        UserLoginGroup(
                            login = login,
                            group = group,
                            cookies = aesEncryption.decryptData(login, cookies) ?: cookies
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }

    data class UserLoginGroup(val login: String, val group: String, val cookies: String? = null)
}

fun main(): Unit = runBlocking {
    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD")
    )
    val aesEncryption = AesEncryption(
        keyStorePath = System.getenv("KS_PATH"),
        keyStorePassword = System.getenv("KS_PASS")
    )
    UserDataTable.selectAllWithCookies(aesEncryption)?.forEach {
        println(it)
    }
}