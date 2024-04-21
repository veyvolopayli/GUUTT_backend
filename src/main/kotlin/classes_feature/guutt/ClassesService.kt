package org.example.classes_feature.guutt

import org.example.classes_feature.data.ClassObject
import org.example.currentSemester
import api.tables.ClassesTable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ClassesService {
    fun updateScheduleOrNothing(group: String, guuWebsiteClasses: List<ClassObject>) {
        val (semesterStart, semesterEnd) = currentSemester() ?: return
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        // For some reason, the schedule on the site contains old classes from 2021, so they need to be removed
        val actualClasses = guuWebsiteClasses.dropWhile {
            val classDate = LocalDate.parse(it.start, formatter)
            classDate < semesterStart
        }

        // if schedule is broken we will return
        if (actualClasses.size < 24) return
        val classesFromDb: List<ClassObject>? = ClassesTable.fetchClassesForPeriod(group, semesterStart, semesterEnd)

        if (actualClasses != classesFromDb) {
            // Update classes
            ClassesTable.updateClasses(group, semesterStart, semesterEnd, actualClasses)
        }
    }
}