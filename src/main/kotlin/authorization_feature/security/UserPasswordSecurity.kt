package org.guutt.authorization_feature.security

import api.authorization.security.AesEncryption
import org.guutt.GuuWebsiteService

class UserPasswordSecurity(private val aesEncryption: AesEncryption, private val guuWebsiteService: GuuWebsiteService) {

}