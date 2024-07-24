package org.guutt.classes_feature.guu

import java.io.File
import java.security.MessageDigest
import java.util.Base64

class Hashing {
    fun hashFileToBase64(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        return encodeData(hash)
    }

    fun hashBytesToBase64(bytes: ByteArray): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        return encodeData(hash)
    }

    private fun encodeData(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decodeData(bytesString: String): ByteArray = Base64.getDecoder().decode(bytesString)
}