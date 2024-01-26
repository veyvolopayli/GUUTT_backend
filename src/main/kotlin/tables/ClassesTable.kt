package org.example.tables

import org.example.ClassDescription
import org.example.ClassObject
import org.example.DbResponse
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
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
                batchInsert(classObjects, true) {
                    this[ClassesTable.id] = it.id
                    this[title] = it.title
                    this[color] = it.color
                    this[start] = it.start
                    this[end] = it.end
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

    fun fetchClasses(group: String): DbResponse<Map<String, List<ClassObject>>> {
        try {
            return transaction {
                val classes = selectAll().where {
                    ClassesTable.group eq group.trim()
                }.map {
                    ClassObject(
                        id = it[ClassesTable.id],
                        title = it[title],
                        color = it[color],
                        start = it[start],
                        end = it[end],
                        description = ClassDescription(
                            building = it[building],
                            classroom = it[classroom],
                            event = it[event],
                            professor = it[professor],
                            department = it[department]
                        )
                    )
                }
                DbResponse.Success(data = classes.groupBy {
                    it.start.dropLast(9)
                })
            }
        } catch (e: Exception) {
            return DbResponse.Error(e.message ?: "Unexpected error")
        }
    }

    fun getSavedGroups(): DbResponse<List<String>> {
        return try {
            transaction {
                val groups = select(group).withDistinct().map { it[group] }
                DbResponse.Success(data = groups)
            }
        } catch (e: Exception) {
            println(e.message)
            DbResponse.Error(e.message ?: "Unexpected error")
        }
    }
}