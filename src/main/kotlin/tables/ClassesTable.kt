package org.example.tables

import org.example.ClassDescription
import org.example.ClassObject
import org.example.DbResponse
import org.example.fillDatesGaps
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ClassesTable : Table("classes") {
    private val idColumn = long("id")
    private val titleColumn = varchar("title", 100)
    private val colorColumn = varchar("color", 7)
    private val startColumn = varchar("start", 19)
    private val endColumn = varchar("end", 19)
    private val buildingColumn = varchar("building", 25)
    private val classroomColumn = varchar("classroom", 10)
    private val eventColumn = varchar("event", 25)
    private val professorColumn = varchar("professor", 100)
    private val departmentColumn = varchar("department", 100)
    private val groupColumn = varchar("group", 100)

    fun insertClasses(studentGroup: String, classObjects: List<ClassObject>): Int? {
        return try {
            transaction {
                batchInsert(classObjects, true) {
                    this[idColumn] = it.id
                    this[titleColumn] = it.title
                    this[colorColumn] = it.color
                    this[startColumn] = it.start
                    this[endColumn] = it.end
                    this[buildingColumn] = it.description.building
                    this[classroomColumn] = it.description.classroom
                    this[eventColumn] = it.description.event
                    this[professorColumn] = it.description.professor
                    this[departmentColumn] = it.description.department
                    this[groupColumn] = studentGroup
                }.count()
            }
        } catch (e: Exception) {
            println(e.message)
            null
        }
    }

    fun fetchGroupedClasses(group: String): DbResponse<Map<String, List<ClassObject>>> {
        return try {
            val classes = fetchClasses(group)
            // Группировка по дате без времени. substringBefore('T'),
            // так как формат времени yyyy-MM-dd'T'HH:mm:ss навряд ли изменится
            DbResponse.Success(data = classes.groupBy { it.start.substringBefore('T') })
        } catch (e: Exception) {
            e.printStackTrace()
            DbResponse.Error(e.message ?: "Unexpected error")
        }
    }

    fun fetchClasses(group: String): List<ClassObject> {
        return try {
            transaction {
                selectAll().where {
                    groupColumn eq group.trim()
                }.orderBy(startColumn).map {
                    ClassObject(
                        id = it[idColumn],
                        title = it[titleColumn],
                        color = it[colorColumn],
                        start = it[startColumn],
                        end = it[endColumn],
                        description = ClassDescription(
                            building = it[buildingColumn],
                            classroom = it[classroomColumn],
                            event = it[eventColumn],
                            professor = it[professorColumn],
                            department = it[departmentColumn]
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun fetchClassesForPeriod(group: String, period: Pair<LocalDate, LocalDate>): List<ClassObject>? {
        return try {
            transaction {
                selectAll().where {
                    (groupColumn eq group) and startColumn.between(
                        period.first.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME),
                        period.second.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME)
                    )
                }.orderBy(startColumn).map {
                    ClassObject(
                        id = it[idColumn],
                        title = it[titleColumn],
                        color = it[colorColumn],
                        start = it[startColumn],
                        end = it[endColumn],
                        description = ClassDescription(
                            building = it[buildingColumn],
                            classroom = it[classroomColumn],
                            event = it[eventColumn],
                            professor = it[professorColumn],
                            department = it[departmentColumn]
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSavedGroups(): DbResponse<List<String>> {
        return try {
            transaction {
                val groups = select(groupColumn).withDistinct().map { it[groupColumn] }
                DbResponse.Success(data = groups)
            }
        } catch (e: Exception) {
            println(e.message)
            DbResponse.Error(e.message ?: "Unexpected error")
        }
    }

    fun updateClasses(group: String, semester: Pair<LocalDate, LocalDate>, newClasses: List<ClassObject>): DbResponse<Unit> {
        return try {
            transaction {
                deleteWhere {
                    (groupColumn eq group) and startColumn.between(
                        semester.first.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME),
                        semester.second.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME)
                    )
                }
            }
            insertClasses(group, newClasses)
            DbResponse.Success(Unit)
        } catch (e: Exception) {
            DbResponse.Error("${e.message}")
        }
    }
}