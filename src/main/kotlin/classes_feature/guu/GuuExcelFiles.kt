package org.guutt.classes_feature.guu

interface GuuExcelFiles {
    fun downloadFiles(): Map<Int, ByteArray>
    fun compareExcelSchedulesWithExisting(excels: Map<Int, ByteArray>): Map<Int, Boolean>
}