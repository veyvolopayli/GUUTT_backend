package org.example.classes_feature.guu

import org.example.common.applyHeaders
import org.example.tables.BachelorExcelsTable
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request

class GuuExcelFilesImpl: GuuExcelFiles {
    override fun downloadFiles(): Map<Int, ByteArray> {
        return listOf(1, 2, 3, 4).associateWith { course ->
            val link = getLink(course + 1)
            val request = Request(Method.GET, link)
                .applyHeaders("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            val response = ApacheClient().invoke(request)
            val stream = response.body.stream
            stream.readBytes().also { stream.close() }
        }
    }

    override fun compareExcelSchedulesWithExisting(excels: Map<Int, ByteArray>): Map<Int, Boolean> {
        if (excels.size < 4) return emptyMap()
        val existingExcelsHashes = BachelorExcelsTable.getHashes()
        val hashing = Hashing()
        val resultMap = mutableMapOf<Int, Boolean>()
        if (existingExcelsHashes.isEmpty()) {
            excels.forEach {
                val hash = hashing.hashBytesToBase64(it.value)
                BachelorExcelsTable.insertHash(course = it.key, hash = hash)
            }
            return mapOf(1 to true, 2 to true, 3 to true, 4 to true)
        } else {
            existingExcelsHashes.forEach {
                val newFileHash = hashing.hashBytesToBase64(excels[it.key]!!)
                if (newFileHash != it.value) {
                    resultMap[it.key] = false
                    BachelorExcelsTable.updateHash(it.key, newFileHash)
                } else {
                    resultMap[it.key] = true
                }
            }
            return resultMap
        }
    }

    private fun getLink(course: Int): String {
        return "https://guu.ru/wp-content/uploads/$course-курс-бакалавриат-ОФО-86.xlsx"
    }
}