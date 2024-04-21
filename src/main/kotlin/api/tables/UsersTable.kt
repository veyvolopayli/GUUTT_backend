package api.tables

import api.authorization.security.AesEncryption
import org.example.common.encodeToBase64
import org.example.common.getCookiesKeyForKeystore
import org.example.logger
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert

object UsersTable : Table("users") {
    val loginColumn = varchar("login", 50).uniqueIndex()
    val passwordColumn = varchar("password", 100)
    val cookiesColumn = varchar("cookies", 255)

    fun insertOrUpdateUser(login: String, password: String, cookies: String): Int? = try {
        transaction {
            upsert {
                it[loginColumn] = login
                it[passwordColumn] = password
                it[cookiesColumn] = cookies
            }.insertedCount
        }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    fun checkUserExistence(login: String): Boolean? = try {
        transaction { select(loginColumn).where { loginColumn eq login }.count() > 0 }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    fun getDecryptedPassword(login: String, aesEncryption: AesEncryption): String? = try {
        val encryptedPass = transaction { select(passwordColumn).where { loginColumn eq login } }.single()[passwordColumn]
        aesEncryption.decryptData(login, encryptedPass)
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    fun getEncryptedCookies(login: String, aesEncryption: AesEncryption): String? = try {
        val cookies = transaction { select(cookiesColumn).where { loginColumn eq login }.single()[cookiesColumn] }
        aesEncryption.decryptData(login, cookies)
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    fun updateCookies(login: String, cookies: String, aesEncryption: AesEncryption): Int? = try {
        val encryptedCookies = aesEncryption.encryptData(getCookiesKeyForKeystore(login), cookies).encodeToBase64()
        transaction { update({ loginColumn eq login }) { it[cookiesColumn] = encryptedCookies } }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    fun deleteUser(login: String): Int? = try {
        transaction { deleteWhere { loginColumn eq login } }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }
}