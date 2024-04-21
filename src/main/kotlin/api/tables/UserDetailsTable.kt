package api.tables

import api.authorization.security.AesEncryption
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.classes_feature.data.ClassObject
import org.example.logger
import org.example.tables.response.DbResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.*
import java.util.*

object UserDetailsTable : Table("user_data") {
    private val loginColumn = varchar("login", 50).uniqueIndex()
    private val fullNameColumn = varchar("full_name", 100)
    private val groupColumn = varchar("group", 100)
    private val registerTimeColumn = long("register_date")
    private val timestampColumn = timestamp("timestamp")
    private val dateColumn = date("date")
    private val timestampWithTimezoneColumn = timestampWithTimeZone("timestamp_timezone")
    private val timeColumn = time("time")
//    private val jsonColumn = json<Classes>("json", Json)
    private val priceTypes = array<String>("price_types")
    private val jsonArray = json<List<ClassObject>>("json", Json)

    fun insertOrUpdateUserDetails(login: String, userDetails: UserDetails): Int? = try {
        transaction {
            upsert {
                it[loginColumn] = login
                it[fullNameColumn] = userDetails.fullName
                it[groupColumn] = userDetails.group
                it[registerTimeColumn] = System.currentTimeMillis()
                it[timestampColumn] = Instant.now()
                it[dateColumn] = LocalDate.now()
                it[timeColumn] = LocalTime.now()
                it[timestampWithTimezoneColumn] = OffsetDateTime.now()
                it[priceTypes] = userDetails.priceTypes
                it[jsonArray] = ClassesTable.fetchClasses("[ИИС] Прикладная информатика 3-1 (2021)")
            }.insertedCount
        }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    fun selectUserDetails(login: String): UserDetails? = try {
        transaction {
            val row = selectAll().where {
                loginColumn eq login
            }.single()
            UserDetails(
                registerDate = row[registerTimeColumn],
                fullName = row[fullNameColumn],
                timestamp = row[timestampColumn],
                date = row[dateColumn],
                group = row[groupColumn],
                time = row[timeColumn],
                priceTypes = row[priceTypes],
                jsonArray = row[jsonArray]
            )
        }
    } catch (e: Exception) {
        logger.error(e.message)
        null
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
}

data class UserLoginGroup(val login: String, val group: String, val cookies: String? = null)

data class UserDetails(
    val fullName: String,
    val group: String,
    val registerDate: Long = System.currentTimeMillis(),
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val timestamp: Instant = Instant.now(),
    val priceTypes: List<String> = emptyList(),
    val jsonArray: List<ClassObject> = emptyList()
)

@Serializable
data class Classes(
    val classObjects: List<ClassObject>
)

fun main() {
    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD")
    )
//    val (semesterStart, semesterEnd) = currentSemester() ?: return@runBlocking
//    ClassesTable.getClassesOfAllGroupsForSemester(semesterStart, semesterEnd)?.forEach {
//        println(it)
//    }
//    val aesEncryption = AesEncryption(
//        keyStorePath = System.getenv("KS_PATH"),
//        keyStorePassword = System.getenv("KS_PASS")
//    )
//    UserDataTable.selectAllWithCookies(aesEncryption)?.forEach {
//        println(it)
//    }

//    println(UsersTable.insertOrUpdateUser(login = "someLogin1", password = "password123", cookies = "nigga"))
//    println(UsersTable.insertOrUpdateUser(login = "someLogin3", cookies = "sfsdfsd"))

    val userDetails = UserDetails(
        fullName = "Ананас",
        group = "Фрукты",
        priceTypes = listOf("р", "р/час", "Бесплатно")
    )

    println(UserDetailsTable.insertOrUpdateUserDetails("someLogin", userDetails))
    println(UserDetailsTable.selectUserDetails("someLogin"))
}