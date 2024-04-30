package org.example.classes_feature.guutt

import api.tables.ClassesTable
import kotlinx.coroutines.coroutineScope
import org.example.GuuWebsiteService
import org.example.classes_feature.data.ClassObject
import org.example.common.results.GuuResponse
import org.example.currentSemester
import org.example.logger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ClassesService(private val guuWebsiteService: GuuWebsiteService) {

    /**
     * Использовать только внутри [transaction]
     */
    fun fetchAndInsertOrUpdateClasses(group: String, cookies: String): List<ClassObject>? {
        val (semesterStart, semesterEnd) = currentSemester() ?: return null
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        when(val guuWebsiteClassesResult = guuWebsiteService.fetchClasses(cookies)) {
            is GuuResponse.Success -> {
                val guuWebsiteClasses = guuWebsiteClassesResult.data ?: return null
                // For some reason, the schedule on the site contains old classes from 2021, so they need to be removed
                val websiteActualClasses = guuWebsiteClasses.sortedBy {
                    LocalDateTime.parse(it.start, formatter)
                }.dropWhile {
                    val classDate = LocalDate.parse(it.start, formatter)
                    classDate < semesterStart
                }.dropLastWhile {
                    val classDate = LocalDate.parse(it.start, formatter)
                    classDate > semesterEnd
                }

                // if schedule is broken we will return
                if (websiteActualClasses.size < 24) return null
                val classesFromDb: List<ClassObject> = ClassesTable.fetchClassesForPeriod(group, semesterStart, semesterEnd) ?: return null

                if (websiteActualClasses != classesFromDb) {
                    // Update classes
                    logger.info("Schedule updated for group: $group")
                    return ClassesTable.updateClasses(group = group, oldClasses = classesFromDb, newClasses = websiteActualClasses)
                }
                return classesFromDb
            }
            else -> {
                return null
            }
        }
    }
}