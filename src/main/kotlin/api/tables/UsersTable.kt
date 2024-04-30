package api.tables

import api.authorization.security.AesEncryption
import org.example.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object UsersTable : Table("users") {
    val loginColumn = varchar("login", 50).uniqueIndex()
    val passwordColumn = varchar("password", 100)
    val cookiesColumn = varchar("cookies", 500)

    /**
     * Объявлять строго внутри транзакции
     */
    fun insertOrUpdateUser(login: String, password: String, cookies: String): Int? = try {
        upsert {
            it[loginColumn] = login
            it[passwordColumn] = password
            it[cookiesColumn] = cookies
        }.insertedCount
    } catch (e: Exception) {
        logger.error(e.stackTraceToString())
        null
    }

    fun checkUserExistence(login: String): Boolean? = try {
        transaction { select(loginColumn).where { loginColumn eq login }.count() > 0 }
    } catch (e: Exception) {
        logger.error(e.stackTraceToString())
        null
    }

    fun getDecryptedPassword(login: String, aesEncryption: AesEncryption): String? = try {
        val encryptedPass = transaction { select(passwordColumn).where { loginColumn eq login }.single()[passwordColumn] }
        aesEncryption.decryptData(login, encryptedPass)
    } catch (e: Exception) {
        logger.error(e.stackTraceToString())
        null
    }

    fun getEncryptedCookies(login: String, aesEncryption: AesEncryption): String? = try {
        val cookies = transaction { select(cookiesColumn).where { loginColumn eq login }.single()[cookiesColumn] }
        aesEncryption.decryptData(login, cookies)
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    suspend fun updateUserCookies(login: String, encryptedCookies: String): Int? = try {
        newSuspendedTransaction { update({ loginColumn eq login }) { it[cookiesColumn] = encryptedCookies } }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    suspend fun deleteUser(login: String): Int? = try {
        newSuspendedTransaction { deleteWhere { loginColumn eq login } }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }
}