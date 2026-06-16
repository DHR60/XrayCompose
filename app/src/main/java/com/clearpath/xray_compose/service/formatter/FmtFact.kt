package com.clearpath.xray_compose.service.formatter

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.enums.EConfigType

object FmtFact {
    fun getUrl(profile: ProfileModel): Result<String> {
        try {
            val fmt = when (profile.configType) {
                EConfigType.VLESS -> VlessFmt()
                EConfigType.HYSTERIA2 -> Hysteria2Fmt()
                EConfigType.TROJAN -> TrojanFmt()
                else -> throw IllegalArgumentException("Unsupported config type: ${profile.configType}")
            }
            return fmt.toUri(profile)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun parseUrl(url: String): Result<ProfileModel> {
        try {
            if (listOf(GlobalConst.protocolSchemeMap[EConfigType.VLESS]!!).any {
                    url.startsWith(
                        it,
                        ignoreCase = true
                    )
                }
            ) {
                return VlessFmt().parse(url)
            } else if (listOf(
                    GlobalConst.protocolSchemeMap[EConfigType.HYSTERIA2]!!,
                    GlobalConst.hysteria2SchemeAlias
                ).any {
                    url.startsWith(
                        it,
                        ignoreCase = true
                    )
                }
            ) {
                return Hysteria2Fmt().parse(url)
            } else if (listOf(GlobalConst.protocolSchemeMap[EConfigType.TROJAN]!!).any {
                    url.startsWith(
                        it,
                        ignoreCase = true
                    )
                }
            ) {
                return TrojanFmt().parse(url)
            } else {
                throw IllegalArgumentException("Unsupported URL scheme: ${url.substringBefore(":")}")
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}