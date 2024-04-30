package org.example.authorization_feature.security

import api.authorization.security.AesEncryption
import org.example.GuuWebsiteService
import org.example.common.results.GuuResponse
import org.example.logger
import api.tables.UserDetailsTable

class UserPasswordSecurity(private val aesEncryption: AesEncryption, private val guuWebsiteService: GuuWebsiteService) {

}