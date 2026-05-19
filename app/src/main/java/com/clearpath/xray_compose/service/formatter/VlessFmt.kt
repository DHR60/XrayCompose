package com.clearpath.xray_compose.service.formatter

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.enums.EConfigType
import com.clearpath.xray_compose.service.formatter.model.EncodedParameters
import io.ktor.http.URLBuilder

class VlessFmt : IFmt {
    override fun parse(str: String): Result<ProfileModel> {
        val url = URLBuilder(str)

        var item = ProfileModel.Empty().copy(
            configType = EConfigType.VLESS,
            address = FmtUtils.tryIDNDecode(url.host),
            port = url.port.takeIf { it != -1 } ?: 443,
            remark = url.fragment,
            password = url.user ?: "",
        )

        val queryParams = url.parameters
        item = item.copy(
            protoExtraRaw = item.protocolExtra.copy(
                flow = queryParams["flow"],
                vlessEncryption = queryParams["encryption"],
            ).toJsonString(),
        )

        item = FmtUtils.parseQueryParams(queryParams.build(), item)

        return Result.success(item)
    }

    override fun toUri(profile: ProfileModel): Result<String> {
        val protoExtra = profile.protocolExtra

        val params = EncodedParameters()
        if (protoExtra.vlessEncryption.isNullOrEmpty()) {
            params["encryption"] = GlobalConst.none
        } else {
            params["encryption"] = protoExtra.vlessEncryption
        }
        if (!protoExtra.flow.isNullOrEmpty()) {
            params["flow"] = protoExtra.flow
        }
        params.appendAll(FmtUtils.toQueryParams(profile).build())

        return Result.success(
            FmtUtils.buildUriByProfile(
                item = profile,
                query = FmtUtils.buildQuery(params),
            )
        )
    }
}