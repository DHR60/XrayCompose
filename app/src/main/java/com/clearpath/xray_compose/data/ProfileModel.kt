package com.clearpath.xray_compose.data

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.db.entities.ProfileItem
import com.clearpath.xray_compose.enums.EConfigType
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.serialization.json.Json

data class ProfileModel(
    val id: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val configVersion: Int = 1,
    val configType: EConfigType = EConfigType.VLESS,

    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Double = createdAt.toDouble(),

    val subId: String = "",
    val isSub: Boolean = false,

    val remark: String = "",
    val address: String = "",
    val port: String = "443",
    val password: String = "",
    val username: String = "",
    val network: String = "",

    val streamSecurity: String = "",
    val allowInsecure: String = "",
    val sni: String = "",
    val alpn: String = "",
    val utlsFingerprint: String = "",
    val realityPublicKey: String = "",
    val realityShortId: String = "",
    val realitySpiderX: String = "",
    val realityMldsa65Verify: String = "",

    val muxEnabled: String = "",
    val cert: String = "",
    val certSha: String = "",
    val echConfigList: String = "",

    val finalmask: String = "",

    val protoExtraRaw: String = "",
    val transportExtraRaw: String = "",
) {
    companion object {
        val Empty = { ProfileModel() }
        fun fromProfileItem(item: ProfileItem): ProfileModel {
            return ProfileModel(
                id = item.id.toString(),
                configVersion = item.configVersion,
                configType = item.configType,
                createdAt = item.createdAt,
                sortOrder = item.sortOrder,
                remark = item.remark,
                subId = item.subId,
                isSub = item.isSub,
                address = item.address,
                port = item.port.toString(),
                password = item.password,
                username = item.username,
                network = item.network,
                streamSecurity = item.streamSecurity,
                allowInsecure = item.allowInsecure,
                sni = item.sni,
                alpn = item.alpn,
                utlsFingerprint = item.utlsFingerprint,
                realityPublicKey = item.realityPublicKey,
                realityShortId = item.realityShortId,
                realitySpiderX = item.realitySpiderX,
                realityMldsa65Verify = item.realityMldsa65Verify,
                muxEnabled = item.muxEnabled,
                cert = item.cert,
                certSha = item.certSha,
                echConfigList = item.echConfigList,
                finalmask = item.finalmask,
                protoExtraRaw = item.protoExtraRaw,
                transportExtraRaw = item.transportExtraRaw,
            )
        }
    }

    fun toProfileItem(): ProfileItem {
        return ProfileItem(
            id = UuidCreator.fromString(id),
            configVersion = configVersion,
            configType = configType,
            createdAt = createdAt,
            sortOrder = sortOrder,
            subId = subId,
            isSub = isSub,
            remark = remark,
            address = address,
            port = port.toIntOrNull() ?: 0,
            password = password,
            username = username,
            network = network,
            streamSecurity = streamSecurity,
            allowInsecure = allowInsecure,
            sni = sni,
            alpn = alpn,
            utlsFingerprint = utlsFingerprint,
            realityPublicKey = realityPublicKey,
            realityShortId = realityShortId,
            realitySpiderX = realitySpiderX,
            realityMldsa65Verify = realityMldsa65Verify,
            muxEnabled = muxEnabled,
            cert = cert,
            certSha = certSha,
            echConfigList = echConfigList,
            finalmask = finalmask,
            protoExtraRaw = protoExtraRaw,
            transportExtraRaw = transportExtraRaw,
        )
    }

    val protocolExtra: ProtocolExtraItem by lazy {
        try {
            // ProtocolExtraItem.fromJson(protoExtraRaw)
            Json.Default.decodeFromString<ProtocolExtraItem>(protoExtraRaw)
        } catch (e: Exception) {
            ProtocolExtraItem()
        }
    }
    val transportExtra: TransportExtraItem by lazy {
        try {
            // TransportExtraItem.fromJson(transportExtraRaw)
            Json.Default.decodeFromString<TransportExtraItem>(transportExtraRaw)
        } catch (e: Exception) {
            TransportExtraItem()
        }
    }

    fun getNetworkType(): String {
        return if (GlobalConst.transportMap.any { it.key == network }) {
            network
        } else {
            GlobalConst.defaultTransportNetwork
        }
    }
}
