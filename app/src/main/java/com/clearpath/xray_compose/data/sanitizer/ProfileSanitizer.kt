package com.clearpath.xray_compose.data.sanitizer

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.enums.EConfigType

object ProfileSanitizer {
    fun sanitize(profile: ProfileModel): ProfileModel {
        return when (profile.configType) {
            EConfigType.VLESS -> sanitizeVless(profile)
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
}