package com.clearpath.xray_compose.service.formatter

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.enums.EConfigType
import com.clearpath.xray_compose.service.formatter.model.EncodedParameters
import io.ktor.http.URLBuilder

class TrojanFmt : IFmt {
    override fun parse(str: String): Result<ProfileModel> {
        val url = URLBuilder(str)

        var item = ProfileModel.Empty().copy(
            configType = EConfigType.TROJAN,
            address = FmtUtils.tryIDNDecode(url.host),
            port = url.port.takeIf { it != -1 } ?: 443,
            remark = url.fragment,
            password = url.user ?: "",
        )

        if (item.password.isEmpty()) {
            error("URI must contain a user as ID/Password")
        }

        val queryParams = url.parameters
        item = FmtUtils.parseQueryParams(queryParams.build(), item)

        return Result.success(item)
    }

    override fun toUri(profile: ProfileModel): Result<String> {
        val params = EncodedParameters()
        params.appendAll(FmtUtils.toQueryParams(profile).build())

        return Result.success(
            FmtUtils.buildUriByProfile(
                item = profile,
                query = FmtUtils.buildQuery(params),
            )
        )
    }
}