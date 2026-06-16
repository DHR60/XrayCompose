package com.clearpath.xray_compose.service.formatter

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.enums.EConfigType
import com.clearpath.xray_compose.service.formatter.model.EncodedParameters
import io.ktor.http.URLBuilder

class Hysteria2Fmt : IFmt {
    override fun parse(str: String): Result<ProfileModel> {
        val url = URLBuilder(str)

        var item = ProfileModel.Empty().copy(
            configType = EConfigType.HYSTERIA2,
            address = FmtUtils.tryIDNDecode(url.host),
            port = url.port.takeIf { it != -1 } ?: 443,
            remark = url.fragment,
            password = url.user ?: "",
        )

        if (item.password.isEmpty()) {
            error("URI must contain a user as ID/Password")
        }

        val queryParams = url.parameters
        item = item.copy(
            protoExtraRaw = item.protocolExtra.copy(
                ports = queryParams["mport"],
                salamanderPass = queryParams["obfs-password"],
            ).toJsonString(),
        )
        if (item.certSha.isEmpty()) {
            item = item.copy(
                certSha = queryParams["pinSHA256"] ?: ""
            )
        }

        item = FmtUtils.parseQueryParams(queryParams.build(), item)

        return Result.success(item)
    }

    override fun toUri(profile: ProfileModel): Result<String> {
        val protoExtra = profile.protocolExtra

        val params = EncodedParameters()
        if (profile.certSha.isNotEmpty()
            && !profile.certSha.contains(",")
        ) {
            params["pinSHA256"] = profile.certSha
        }
        if (!protoExtra.ports.isNullOrEmpty()) {
            params["mport"] = protoExtra.ports
        }
        if (!protoExtra.salamanderPass.isNullOrEmpty()) {
            params["obfs-password"] = protoExtra.salamanderPass
        }
        params.appendAll(FmtUtils.toQueryParamsLite(profile).build())

        return Result.success(
            FmtUtils.buildUriByProfile(
                item = profile,
                query = FmtUtils.buildQuery(params),
            )
        )
    }
}