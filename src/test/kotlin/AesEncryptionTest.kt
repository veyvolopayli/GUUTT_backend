import api.authorization.security.AesEncryption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AesEncryptionTest {
    private val aesEncryption = AesEncryption(keyStorePath = "keystore.jks", keyStorePassword = System.getenv("KS_PASS"))

    @Test
    fun testLengthOfDifferentPasswordsHashes() {
        val shortPassword = "short"
        val longPassword = "this_is_a_very_long_password"

        val encryptedShortPasswordHash = aesEncryption.encryptData("test_user_1", shortPassword)
        val encodedShortHash = aesEncryption.encodeToBase64(encryptedShortPasswordHash)

        val encryptedLongPasswordHash = aesEncryption.encryptData("test_user_2", longPassword)
        val encodedLongHash = aesEncryption.encodeToBase64(encryptedLongPasswordHash)

        assertNotEquals(
            encodedShortHash.length,
            encodedLongHash.length,
            "Encrypted passwords of different lengths should have the same length"
        )
    }
}