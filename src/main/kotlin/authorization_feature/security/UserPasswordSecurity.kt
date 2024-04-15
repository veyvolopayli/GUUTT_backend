package org.example.authorization_feature.security

import api.authorization.security.AesEncryption
import org.example.GuuWebsiteService
import org.example.common.results.GuuResponse
import org.example.logger
import org.example.tables.UserDataTable
import org.example.tables.UsersTable
import org.example.tables.response.DbResponse

class UserPasswordSecurity(private val aesEncryption: AesEncryption, private val guuWebsiteService: GuuWebsiteService) {
    fun saveSecuredPassword(login: String, password: String, cookies: String): Boolean {
        return try {
            val passBytes = aesEncryption.encryptData(login, password)
            val encodedPass = aesEncryption.encode(passBytes)

            val cookiesBytes = aesEncryption.encryptData(login + "_cookies", cookies)
            val encodedCookies = aesEncryption.encode(cookiesBytes)

            when(UsersTable.insertData(login, encodedPass, encodedCookies)) {
                is DbResponse.Success -> {
                    true
                }
                is DbResponse.Error -> {
                    false
                }
            }
        } catch (e: Exception) {
            logger.error(e.message)
            false
        }
    }

    fun getAndSaveUserInfo(login: String, cookies: String): String? {
        return when(val userInfo = guuWebsiteService.getUserInfo(cookies)) {
            is GuuResponse.Success -> {
                when(UserDataTable.insertData(login = login, userInfo = userInfo.data)) {
                    is DbResponse.Success -> {
                        userInfo.data.group
                    }
                    is DbResponse.Error -> {
                        null
                    }
                }
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