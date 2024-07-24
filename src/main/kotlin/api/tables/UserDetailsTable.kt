package api.tables

import api.authorization.security.AesEncryption
import org.guutt.common.prefixForLoginCookies
import org.guutt.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object UserDetailsTable : Table("user_data") {
    private val loginColumn = varchar("login", 50).uniqueIndex()
    private val fullNameColumn = varchar("full_name", 100)
    private val groupColumn = varchar("group", 100)
    private val registerTimeColumn = long("register_date")

    /**
     * Объявлять строго внутри транзакции
     */
    fun insertOrUpdateUserDetails(login: String, userDetails: UserDetails): Int? = try {
        upsert {
            it[loginColumn] = login
            it[fullNameColumn] = userDetails.fullName
            it[groupColumn] = userDetails.group
            it[registerTimeColumn] = System.currentTimeMillis()
        }.insertedCount
    } catch (e: Exception) {
        logger.error(e.stackTraceToString())
        null
    }

    fun selectUserDetails(login: String): UserDetails? = try {
        transaction {
            val row = selectAll().where {
                loginColumn eq login
            }.single()
            UserDetails(
                timestamp = row[registerTimeColumn],
                fullName = row[fullNameColumn],
                group = row[groupColumn],
            )
        }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    fun selectAllUserGroupWithCookiesDecrypted(aesEncryption: AesEncryption): List<UserGroupWithCookies>? = try {
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
                    UserGroupWithCookies(
                        login = login,
                        group = group,
                        cookies = aesEncryption.decryptData(prefixForLoginCookies(login), cookies) ?: cookies
                    )
                }
        }
    } catch (e: Exception) {
        logger.error(e.stackTraceToString())
        null
    }
}

data class UserGroupWithCookies(val login: String, val group: String, val cookies: String)

data class UserDetails(
    val fullName: String,
    val group: String,
    val timestamp: Long = System.currentTimeMillis(),
)