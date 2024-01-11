package org.example

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

object ClassesTable : Table("classes") {
    private val id = long("id")
    private val title = varchar("title", 100)
    private val color = varchar("color", 7)
    private val start = varchar("start", 19)
    private val end = varchar("end", 19)
    private val building = varchar("building", 25)
    private val classroom = varchar("classroom", 10)
    private val event = varchar("event", 25)
    private val professor = varchar("professor", 100)
    private val department = varchar("department", 100)
    private val group = varchar("group", 100)

    fun insertClasses(studentGroup: String, classObjects: List<ClassObject>): Int? {
        return try {
            transaction {
                batchInsert(classObjects) {
                    this[ClassesTable.id] = it.id
                    this[title] = it.title
                    this[color] = it.color
                    this[start] = it.start.toString()
                    this[end] = it.end.toString()
                    this[building] = it.description.building
                    this[classroom] = it.description.classroom
                    this[event] = it.description.event
                    this[professor] = it.description.professor
                    this[department] = it.description.department
                    this[group] = studentGroup
                }.count()
            }
        } catch (e: Exception) {
            println(e.message)
            null
        }
    }
}