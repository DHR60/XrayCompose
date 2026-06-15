package com.clearpath.xray_compose.service.formatter

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.TransportExtraItem
import com.clearpath.xray_compose.enums.EConfigType
import com.clearpath.xray_compose.enums.ETransport
import com.clearpath.xray_compose.enums.ETransport.GRPC
import com.clearpath.xray_compose.enums.ETransport.HTTPUPGRADE
import com.clearpath.xray_compose.enums.ETransport.KCP
import com.clearpath.xray_compose.enums.ETransport.RAW
import com.clearpath.xray_compose.enums.ETransport.WS
import com.clearpath.xray_compose.enums.ETransport.XHTTP
import com.clearpath.xray_compose.service.formatter.model.EncodedParameters
import com.clearpath.xray_compose.utils.JsonUtil
import com.clearpath.xray_compose.utils.LogUtil
import com.clearpath.xray_compose.utils.Utils
import io.ktor.util.StringValues
import java.net.IDN
import java.util.Locale
import kotlin.enums.enumEntries

object FmtUtils {
    fun buildAuthority(host: String, port: Int?): String {
        return buildAuthority("", host, port)
    }

    fun buildAuthority(userinfo: String, host: String, port: Int?): String {
        return buildAuthorityInner(
            userinfoEncoded = Utils.encodeURIComponent(userinfo),
            host = tryIDNEncode(host),
            port = port
        )
    }

    fun buildAuthorityByProfile(item: ProfileModel): String {
        val userinfo =
            if (item.username.isNotEmpty()) Utils.encodeURIComponent(item.username) + ":" + Utils.encodeURIComponent(
                item.password
            ) else Utils.encodeURIComponent(item.password)
        return buildAuthority(userinfo, item.address, item.port)
    }

    private fun buildAuthorityInner(userinfoEncoded: String, host: String, port: Int?): String {
        val authority = StringBuilder()
        if (userinfoEncoded.isNotEmpty()) {
            authority.append("$userinfoEncoded@")
        }
        authority.append(host)
        if (port != null) {
            authority.append(":$port")
        }
        return authority.toString()
    }

    fun buildQuery(params: EncodedParameters): String {
        return params.buildQuery()
    }

    fun buildUriByProfile(
        item: ProfileModel,
        query: String
    ): String {
        return buildUri(
            configType = item.configType,
            authority = buildAuthorityByProfile(item),
            query = query,
            remark = item.remark
        )
    }

    fun buildUri(
        configType: EConfigType,
        authority: String,
        query: String,
        remark: String
    ): String {
        return buildUri(
            GlobalConst.protocolSchemeMap[configType] ?: error(
                "Invalid config type: $configType"
            ), authority, "", query, remark
        )
    }

    fun buildUri(
        scheme: String,
        authority: String,
        path: String,
        query: String,
        fragment: String
    ): String {
        val uri = StringBuilder()
        uri.append("$scheme://")
        uri.append(authority)
        if (path.isNotEmpty()) {
            if (!path.startsWith("/")) {
                uri.append("/")
            }
            uri.append(path)
        }
        if (query.isNotEmpty()) {
            uri.append("?")
            uri.append(query)
        }
        if (fragment.isNotEmpty()) {
            uri.append("#")
            uri.append(Utils.encodeURIComponent(fragment))
        }
        return uri.toString()
    }

    fun tryIDNEncode(host: String): String {
        if (host.isBlank()) return host
        if (host.all { it.code in 0..127 }) {
            return host
        }
        return try {
            IDN.toASCII(host)
        } catch (e: Exception) {
            LogUtil.e("IDN encoding failed for host: $host", e)
            host
        }
    }

    fun tryIDNDecode(host: String): String {
        if (host.isBlank()) return host
        if (host.all { it.code in 0..127 }) {
            return host
        }
        return try {
            IDN.toUnicode(host)
        } catch (e: Exception) {
            LogUtil.e("IDN decoding failed for host: $host", e)
            host
        }
    }

    fun toQueryParamsLite(item: ProfileModel): EncodedParameters {
        val params = EncodedParameters()
        params.encodeAndAppendIfNotEmpty("sni", item.sni)
        params.encodeAndAppendIfNotEmpty("alpn", item.alpn)
        if (item.allowInsecure == GlobalConst.trueStr) {
            params["allowInsecure"] = "1"
            params["insecure"] = "1"
        }
        return params
    }

    fun toQueryParams(item: ProfileModel): EncodedParameters {
        val params = EncodedParameters()
        val transportParams = transportToQueryParams(item)
        val securityParams = securityToQueryParams(item)
        params.appendAll(transportParams.build())
        params.appendAll(securityParams.build())
        if (item.finalmask.isNotEmpty()) {
            val compressFinalmask = JsonUtil.compressJson(item.finalmask)
            params.encodeAndAppend("fm", compressFinalmask)
        }
        return params
    }

    private fun transportToQueryParams(item: ProfileModel): EncodedParameters {
        val params = EncodedParameters()
        val itemNetwork = item.network.ifEmpty { GlobalConst.defaultTransportNetworkEnum.value }
        val transportType =
            enumEntries<ETransport>().firstOrNull {
                it.value.equals(
                    itemNetwork,
                    ignoreCase = true
                )
            }
        if (transportType == null) {
            return params
        }
        if (transportType == RAW) {
            params["type"] = GlobalConst.rawTransportNetworkAlias
        } else {
            params["type"] = itemNetwork.lowercase(Locale.ROOT)
        }
        val transportExtra = item.transportExtra
        when (transportType) {
            RAW -> {
                if (transportExtra.rawHeaderType.isNullOrEmpty()) {
                    params.encodeAndAppend("headerType", GlobalConst.none)
                } else {
                    params.encodeAndAppend("headerType", transportExtra.rawHeaderType)
                    params.encodeAndAppendIfNotEmpty("host", transportExtra.host)
                    params.encodeAndAppendIfNotEmpty("path", transportExtra.path)
                }
            }

            XHTTP -> {
                params.encodeAndAppendIfNotEmpty("host", transportExtra.host)
                params.encodeAndAppendIfNotEmpty("path", transportExtra.path)
                params.encodeAndAppendIfNotEmpty("mode", transportExtra.xhttpMode)
                if (!transportExtra.xhttpExtra.isNullOrEmpty()) {
                    val compressXhttpExtra = JsonUtil.compressJson(transportExtra.xhttpExtra)
                    params.encodeAndAppend("extra", compressXhttpExtra)
                }
            }

            WS, HTTPUPGRADE -> {
                params.encodeAndAppendIfNotEmpty("host", transportExtra.host)
                params.encodeAndAppendIfNotEmpty("path", transportExtra.path)
            }

            GRPC -> {
                params.encodeAndAppendIfNotEmpty("authority", transportExtra.grpcAuthority)
                params.encodeAndAppendIfNotEmpty("serviceName", transportExtra.grpcServiceName)
                params.encodeAndAppend(
                    "mode",
                    (if (GlobalConst.grpcModeList.contains(transportExtra.grpcMode))
                        transportExtra.grpcMode else GlobalConst.defaultGrpcMode)!!
                )
            }

            KCP -> {
                if (transportExtra.kcpHeaderType.isNullOrEmpty()) {
                    params.encodeAndAppend("headerType", GlobalConst.none)
                } else {
                    params.encodeAndAppend("headerType", transportExtra.kcpHeaderType)
                }
                params.encodeAndAppendIfNotEmpty("seed", transportExtra.kcpSeed)
                params.encodeAndAppendIfNotEmpty("mtu", transportExtra.kcpMtu)
            }
        }
        return params
    }

    private fun securityToQueryParams(item: ProfileModel): EncodedParameters {
        val params = EncodedParameters()
        if (item.streamSecurity.isEmpty()) {
            return params
        }
        params["security"] = item.streamSecurity
        params.encodeAndAppendIfNotEmpty("sni", item.sni)
        params.encodeAndAppendIfNotEmpty("fp", item.utlsFingerprint)
        if (item.streamSecurity == GlobalConst.transportSecurityTls) {
            params.encodeAndAppendIfNotEmpty("alpn", item.alpn)
            if (item.allowInsecure == GlobalConst.trueStr) {
                params["allowInsecure"] = "1"
                params["insecure"] = "1"
            }
            params.encodeAndAppendIfNotEmpty("ech", item.echConfigList)
            params.encodeAndAppendIfNotEmpty("vcn", item.certVerifyName)
            params.encodeAndAppendIfNotEmpty("pcs", item.certSha)
        } else if (item.streamSecurity == GlobalConst.transportSecurityReality) {
            params.encodeAndAppendIfNotEmpty("pbk", item.realityPublicKey)
            params.encodeAndAppendIfNotEmpty("sid", item.realityShortId)
            params.encodeAndAppendIfNotEmpty("spx", item.realitySpiderX)
            params.encodeAndAppendIfNotEmpty("pqv", item.realityMldsa65Verify)
        }
        return params
    }

    fun parseQueryParams(
        params: StringValues,
        baseItem: ProfileModel = ProfileModel.Empty()
    ): ProfileModel {
        var item = baseItem
        item = parseTransportParams(params, item)
        item = parseSecurityParams(params, item)
        if (params.contains("fm")) {
            item = item.copy(finalmask = JsonUtil.prettyJson(params["fm"]!!))
        }
        return item;
    }

    private fun parseTransportParams(params: StringValues, baseItem: ProfileModel): ProfileModel {
        var item = baseItem
        val transportType = params["type"] ?: GlobalConst.defaultTransportNetwork
        val transportTypeNormalized =
            if (transportType.equals(GlobalConst.rawTransportNetworkAlias, ignoreCase = true)) {
                GlobalConst.defaultTransportNetwork
            } else {
                transportType
            }
        val transportEnum = enumEntries<ETransport>().firstOrNull {
            it.value.equals(
                transportTypeNormalized,
                ignoreCase = true
            )
        }
            ?: error("Invalid transport type: $transportTypeNormalized")
        item = item.copy(network = transportEnum.value)
        when (transportEnum) {
            RAW -> {
                val headerType = params["headerType"] ?: GlobalConst.none
                item = item.copy(
                    transportExtraRaw =
                        TransportExtraItem(
                            rawHeaderType = headerType,
                            host = params["host"] ?: "",
                            path = params["path"] ?: "",
                        ).toJsonString()
                )
            }

            XHTTP -> {
                item = item.copy(
                    transportExtraRaw =
                        TransportExtraItem(
                            xhttpMode = params["mode"] ?: GlobalConst.defaultXhttpMode,
                            host = params["host"] ?: "",
                            path = params["path"] ?: "",
                            xhttpExtra = if (params.contains("extra")) JsonUtil.prettyJson(
                                params["extra"]!!
                            ) else ""
                        ).toJsonString()
                )
            }

            WS, HTTPUPGRADE -> {
                item = item.copy(
                    transportExtraRaw =
                        TransportExtraItem(
                            host = params["host"] ?: "",
                            path = params["path"] ?: "",
                        ).toJsonString()
                )
            }

            GRPC -> {
                item = item.copy(
                    transportExtraRaw =
                        TransportExtraItem(
                            grpcAuthority = params["authority"] ?: "",
                            grpcServiceName = params["serviceName"] ?: "",
                            grpcMode = if (params.contains("mode") && GlobalConst.grpcModeList.contains(
                                    params["mode"]
                                )
                            ) params["mode"]!! else GlobalConst.defaultGrpcMode,
                        ).toJsonString()
                )
            }

            KCP -> {
                val headerType = params["headerType"] ?: GlobalConst.none
                item = item.copy(
                    transportExtraRaw =
                        TransportExtraItem(
                            kcpHeaderType = headerType,
                            kcpSeed = params["seed"] ?: "",
                            kcpMtu = params["mtu"] ?: "",
                        ).toJsonString()
                )
            }
        }
        return item
    }

    private fun parseSecurityParams(params: StringValues, baseItem: ProfileModel): ProfileModel {
        var item = baseItem
        if (!params.contains("security")) {
            return item
        }
        val securityType = params["security"] ?: ""
        item = item.copy(
            streamSecurity = securityType,
            sni = params["sni"] ?: "",
            utlsFingerprint = params["fp"] ?: "",
        )
        if (securityType == GlobalConst.transportSecurityTls) {
            item = item.copy(
                alpn = params["alpn"] ?: "",
                allowInsecure = if (params.contains("allowInsecure") && params["allowInsecure"] == "1") GlobalConst.trueStr else GlobalConst.falseStr,
                echConfigList = params["ech"] ?: "",
                certVerifyName = params["vcn"] ?: "",
                certSha = params["pcs"] ?: "",
            )
        } else if (securityType == GlobalConst.transportSecurityReality) {
            item = item.copy(
                realityPublicKey = params["pbk"] ?: "",
                realityShortId = params["sid"] ?: "",
                realitySpiderX = params["spx"] ?: "",
                realityMldsa65Verify = params["pqv"] ?: "",
            )
        }
        return item
    }
}