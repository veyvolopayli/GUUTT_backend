package org.example.authorization_feature.security

import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class AesEncryption {
    private val cipher = Cipher.getInstance("AES")
    private val keyStore = KeyStore.getInstance("JCEKS").apply {
        val fis = FileInputStream("keystore.jks")
        load(fis, "pass123".toCharArray())
    }

    fun encryptPassword(userId: String, password: String): ByteArray {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256, SecureRandom())
        val key = keyGenerator.generateKey()
        cipher.init(Cipher.ENCRYPT_MODE, key)
        saveSecretKey(userId, key)
        return cipher.doFinal(password.toByteArray())
    }

    fun decryptPassword(userId: String, encryptedPass: ByteArray): String {
        val key = getSecretKey(userId)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return String(cipher.doFinal(encryptedPass))
    }

    private fun saveSecretKey(keyId: String, key: SecretKey) {
        val entry = SecretKeyEntry(key)
        keyStore.setEntry(keyId, entry, KeyStore.PasswordProtection("12345".toCharArray()))
        val fos = FileOutputStream("keystore.jks")
        keyStore.store(fos, "pass123".toCharArray())
    }

    private fun getSecretKey(keyId: String): SecretKey? {
        val entry = keyStore.getEntry(keyId, KeyStore.PasswordProtection("12345".toCharArray())) as? SecretKeyEntry
        return entry?.secretKey
    }

    fun encode(data: ByteArray): String = Base64.getEncoder().encodeToString(data)

    fun decode(data: String): ByteArray = Base64.getDecoder().decode(data)
}

fun main() {
    val aesEncryption = AesEncryption()
//    val ePass = aesEncryption.encryptPassword("someUserId", "password123")
//    val stringEPass = aesEncryption.encode(ePass)
//    println(stringEPass)
//    println(ePass)
    val dPass = aesEncryption.decryptPassword("someUserId", aesEncryption.decode("9jw8I7THqVAUf1obaOAaKg=="))
    println(dPass)
}