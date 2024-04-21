package org.example.authorization_feature.security

import api.authorization.security.AesEncryption
import org.example.GuuWebsiteService
import org.example.common.results.GuuResponse
import org.example.logger
import api.tables.UserDetailsTable

class UserPasswordSecurity(private val aesEncryption: AesEncryption, private val guuWebsiteService: GuuWebsiteService) {
    fun saveSecuredPassword(login: String, password: String, cookies: String): Boolean {
        return try {
            val passBytes = aesEncryption.encryptData(login, password)
            val encodedPass = aesEncryption.encodeToBase64(passBytes)

            val cookiesBytes = aesEncryption.encryptData(login + "_cookies", cookies)
            val encodedCookies = aesEncryption.encodeToBase64(cookiesBytes)

//            when(UsersTable.insertData(login, encodedPass, encodedCookies)) {
//                is DbResponse.Success -> {
//                    true
//                }
//                is DbResponse.Error -> {
//                    false
//                }
//            }
            false
        } catch (e: Exception) {
            logger.error(e.message)
            false
        }
    }

    fun getAndSaveUserInfo(login: String, cookies: String): String? {
        return when(val userInfo = guuWebsiteService.getUserDetails(cookies)) {
            is GuuResponse.Success -> {
                val insertedCount = UserDetailsTable.insertOrUpdateUserDetails(login = login, userDetails = userInfo.data) ?: -1
                if (insertedCount > 0) userInfo.data.group else null
            }
            is GuuResponse.NotResponding -> {
                null
            }
            is GuuResponse.Forbidden -> {
                null
            }
            is GuuResponse.CookieExpired -> {
                null
            }
        }
    }
}