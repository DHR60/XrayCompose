package com.clearpath.xray_compose.service.formatter

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.enums.EConfigType

object FmtFact {
    fun getUrl(profile: ProfileModel): Result<String> {
        try {
            val fmt = when (profile.configType) {
                EConfigType.VLESS -> VlessFmt()
                else -> throw IllegalArgumentException("Unsupported config type: ${profile.configType}")
            }
            return fmt.toUri(profile)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun parseUrl(url: String): Result<ProfileModel> {
        try {
            if (url.startsWith(
                    GlobalConst.protocolSchemeMap[EConfigType.VLESS]!!,
                    ignoreCase = true
                )
            )
                return VlessFmt().parse(url)
            else
                throw IllegalArgumentException("Unsupported URL scheme: ${url.substringBefore(":")}")
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}