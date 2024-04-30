package org.example

import api.tables.ClassesTable
import api.tables.UserDetails
import api.tables.UserDetailsTable
import api.tables.UsersTable
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class Testing {


}

suspend fun main(): Unit = coroutineScope {
    val dbConfig = DatabaseConfig {
        useNestedTransactions = false
    }
    Database.connect(
        url = "jdbc:postgresql://5.181.255.253:5432/guutt", driver = "org.postgresql.Driver",
        user = System.getenv("POSTGRES_USERNAME"), password = System.getenv("POSTGRES_PASSWORD"),
        databaseConfig = dbConfig
    )
//    val (semesterStart, semesterEnd) = currentSemester() ?: return@coroutineScope
//    println(ClassesTable.fetchClassesForPeriod("djnfsdnfj", semesterStart, semesterEnd))

//    newSuspendedTransaction {
//        val save1 = UsersTable.insertOrUpdateUser("user23", "user23Password", "cookies123")
//        val save2 = UserDetailsTable.insertOrUpdateUserDetails("user23user23user23user23user23user23user23user23user23user23user23user23user23user23user23user23user23user23user23user23user23", UserDetails("Илья", "Бездари"))
//    }

}