package api.tables

import org.guutt.classes_feature.data.ClassDescription
import org.guutt.classes_feature.data.ClassObject
import org.guutt.logger
import org.guutt.tables.response.DbResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
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

    fun insertClasses(studentGroup: String, classObjects: List<ClassObject>): List<ClassObject>? = try {
        transaction {
            val insertedCount = batchInsert(classObjects) {
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
            if (insertedCount > 24) {
                classObjects
            } else {
                rollback()
                null
            }
        }
    } catch (e: Exception) {
        logger.error(e.stackTraceToString())
        null
    }

    /**
     * Возвращает списки пар, сгруппированные по датам
     */
    fun fetchGroupedClasses(group: String): Map<String, List<ClassObject>>? {
        return try {
            val classes = fetchClasses(group) ?: return null
            // Группировка по дате без времени. substringBefore('T'),
            // так как формат времени yyyy-MM-dd'T'HH:mm:ss навряд ли изменится
            classes.groupBy { it.start.substringBefore('T') }
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
            null
        }
    }

    fun fetchGroupedClassesForPeriod(group: String, start: LocalDate, end: LocalDate): Map<String, List<ClassObject>>? {
        return try {
            val classes = fetchClassesForPeriod(group, start, end) ?: return null
            classes.groupBy { it.start.substringBefore('T') }
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
            null
        }
    }

    fun fetchClasses(group: String): List<ClassObject>? = try {
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
        null
    }

    /**
     * Объявлять исключительно внутри [transaction], иначе всегда будет возвращать null из-за ошибки.
     * @return В случае ошибки null. В случае отсутствия расписания для группы в базе данных вернет пустой список.
     */
    fun fetchClassesForPeriod(group: String, semesterStart: LocalDate, semesterEnd: LocalDate): List<ClassObject>? {
        return try {
            transaction {
                selectAll().where {
                    (groupColumn eq group) and startColumn.between(
                        semesterStart.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME),
                        semesterEnd.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME)
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
            logger.error(e.stackTraceToString())
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
            println(e.stackTraceToString())
            DbResponse.Error(e.message ?: "Unexpected error")
        }
    }

    /**
     * Объявлять исключительно внутри [transaction], иначе всегда будет возвращать null из-за ошибки.
     * @return В случае ошибки вернет null, иначе [newClasses]
     */
    fun updateClasses(
        group: String,
        oldClasses: List<ClassObject>,
        newClasses: List<ClassObject>
    ): List<ClassObject>? = try {
//        transaction {
//            deleteWhere {
//                (groupColumn eq group) and startColumn.between(
//                    semesterStart.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME),
//                    semesterEnd.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME)
//                )
//            }
//        }
        deleteWhere {
            idColumn inList oldClasses.map { it.id }
        }
        insertClasses(group, newClasses)
    } catch (e: Exception) {
        logger.error(e.stackTraceToString())
        null
    }

    fun getClassesOfAllGroupsForSemester(
        semesterStart: LocalDate,
        semesterEnd: LocalDate
    ): Map<String, List<ClassObject>>? = try {
        transaction {
            selectAll().where {
                startColumn.between(
                    semesterStart.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME),
                    semesterEnd.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }.orderBy(startColumn).map { row ->
                ClassObject(
                    id = row[idColumn],
                    title = row[titleColumn],
                    color = row[colorColumn],
                    start = row[startColumn],
                    end = row[endColumn],
                    description = ClassDescription(
                        building = row[buildingColumn],
                        classroom = row[classroomColumn],
                        event = row[eventColumn],
                        professor = row[professorColumn],
                        department = row[departmentColumn]
                    ),
                    group = row[groupColumn]
                )
            }.groupBy { it.group!! } // Да, я уверен, что группа тут не может быть нуллом
        }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }

    fun getGroupsWithClassesCount(semesterStart: LocalDate, semesterEnd: LocalDate): Map<String, Long>? = try {
        transaction {
            select(groupColumn, startColumn.count()).where {
                startColumn.between(
                    semesterStart.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME),
                    semesterEnd.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }.groupBy(groupColumn).associate { row ->
                row[groupColumn] to row[startColumn.count()]
            }
        }
    } catch (e: Exception) {
        logger.error(e.message)
        null
    }
}