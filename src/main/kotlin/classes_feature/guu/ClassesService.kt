package org.example.classes_feature.guu

import org.example.ClassObject
import org.example.currentSemester
import org.example.tables.ClassesTable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ClassesService {
    fun updateScheduleOrNothing(group: String, classDtos: List<ClassObject>) {
        if (classDtos.size < 24) return
        val currentSemester = currentSemester() ?: return
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val actualClasses = classDtos.dropWhile {
            val classDate = LocalDate.parse(it.start, formatter)
            classDate < currentSemester.first
        }
        val classesFromDb = ClassesTable.fetchClassesForPeriod(group, currentSemester) // ClassObject

        if (actualClasses != classesFromDb) {
            // Update classes
            ClassesTable.updateClasses(group, currentSemester, actualClasses)
        }
    }
}