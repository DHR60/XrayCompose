package com.clearpath.xray_compose.data.sanitizer

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.enums.EConfigType

object ProfileSanitizer {
    fun sanitize(profile: ProfileModel): ProfileModel {
        return when (profile.configType) {
            EConfigType.VLESS -> sanitizeVless(profile)
            EConfigType.HYSTERIA2 -> sanitizeHysteria2(profile)
            EConfigType.TROJAN -> sanitizeTrojan(profile)
            else -> sanitizeCommon(profile)
        }
    }

    fun sanitize(profiles: List<ProfileModel>): List<ProfileModel> {
        return profiles.map { sanitize(it) }
    }

    private fun sanitizeCommon(profile: ProfileModel): ProfileModel {
        return profile.copy(
            address = profile.address.trim(),
            password = profile.password.trim(),
            remark = profile.remark.trim(),
            streamSecurity = profile.streamSecurity.trim(),
            sni = profile.sni.trim(),
        )
    }

    private fun sanitizeVless(profile: ProfileModel): ProfileModel {
        var sanProfile = sanitizeCommon(profile)
        sanProfile = sanProfile.copy(
            protoExtraRaw = sanProfile.protocolExtra.copy(
                flow = if (GlobalConst.vlessFlowList.contains(sanProfile.protocolExtra.flow)) sanProfile.protocolExtra.flow else GlobalConst.vlessFlowList.first(),
                vlessEncryption = (sanProfile.protocolExtra.vlessEncryption ?: "").trim()
                    .ifEmpty { GlobalConst.none },
            ).toJsonString()
        )
        return sanProfile
    }

    private fun sanitizeHysteria2(profile: ProfileModel): ProfileModel {
        var sanProfile = sanitizeCommon(profile)
        sanProfile = sanProfile.copy(
            network = GlobalConst.defaultTransportNetwork,
            streamSecurity = sanProfile.streamSecurity.ifEmpty {
                GlobalConst.transportSecurityTls
            },
            alpn = "",
            protoExtraRaw = sanProfile.protocolExtra.copy(
                salamanderPass = (sanProfile.protocolExtra.salamanderPass ?: "").trim(),
                ports = (sanProfile.protocolExtra.ports ?: "").trim(),
                hopInterval = (sanProfile.protocolExtra.hopInterval ?: "").trim(),
            ).toJsonString()
        )
        return sanProfile
    }

    private fun sanitizeTrojan(profile: ProfileModel): ProfileModel {
        var sanProfile = sanitizeCommon(profile)
        sanProfile = sanProfile.copy(
            streamSecurity = sanProfile.streamSecurity.ifEmpty {
                GlobalConst.transportSecurityTls
            }
        )
        return sanProfile
    }
}