package api.authorization.security

import org.example.logger
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.jvm.Throws


open class AesEncryption(private val keyStorePath: String, keyStorePassword: String) {
    private val keyGen = KeyGenerator.getInstance("AES").also { it.init(256, SecureRandom()) }
    private val cipher = Cipher.getInstance("AES")
    private val keyStore = KeyStore.getInstance("JCEKS")
    private val kPassArray = keyStorePassword.toCharArray()

    init {
        try {
            val fis = FileInputStream(keyStorePath)
            keyStore.load(fis, kPassArray)
            fis.close()
        } catch (_: FileNotFoundException) {
            keyStore.load(null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun encryptData(uniqueKey: String, data: String): ByteArray {
        val key = keyGen.generateKey()
        saveSecretKey(uniqueKey, key)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data.toByteArray())
    }

    fun decryptData(uniqueKey: String, encryptedData: String): String? {
        val key = getSecretKey(uniqueKey) ?: return null
        cipher.init(Cipher.DECRYPT_MODE, key)
        val dataBytes = try {
            decodeFromBase64(encryptedData)
        } catch (e: Exception) {
            logger.error(e.stackTraceToString())
            return null
        }
        return String(cipher.doFinal(dataBytes))
    }

    private fun saveSecretKey(keyId: String, key: SecretKey) {
        val entry = SecretKeyEntry(key)
        keyStore.setEntry(keyId, entry, KeyStore.PasswordProtection(kPassArray))
        val fos = FileOutputStream(keyStorePath)
        keyStore.store(fos, kPassArray).also { fos.close() }
    }

    private fun getSecretKey(keyId: String): SecretKey? {
        return keyStore.getKey(keyId, kPassArray) as? SecretKey
    }

    fun encodeToBase64(data: ByteArray): String = Base64.getEncoder().encodeToString(data)

    @Throws(IllegalArgumentException::class)
    fun decodeFromBase64(data: String): ByteArray = Base64.getDecoder().decode(data)
}