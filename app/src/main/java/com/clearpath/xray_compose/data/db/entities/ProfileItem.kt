package com.clearpath.xray_compose.data.db.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.clearpath.xray_compose.data.ProtocolExtraItem
import com.clearpath.xray_compose.data.TransportExtraItem
import com.clearpath.xray_compose.enums.EConfigType
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.serialization.json.Json
import java.util.UUID

@Entity(tableName = "profiles")
data class ProfileItem(
    @PrimaryKey
    val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    val configType: EConfigType,
    val configVersion: Int = 1,
    val subId: String,
    val isSub: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Double = createdAt.toDouble(),

    val remark: String,
    val address: String,
    val port: Int,
    val password: String,
    val username: String,
    val network: String,

    val streamSecurity: String,
    val allowInsecure: String,
    val sni: String,
    val alpn: String,
    val utlsFingerprint: String,
    val realityPublicKey: String,
    val realityShortId: String,
    val realitySpiderX: String,
    val realityMldsa65Verify: String,

    val muxEnabled: String,
    val cert: String,
    val certSha: String,
    val echConfigList: String,

    val finalmask: String,

    val protoExtraRaw: String,
    val transportExtraRaw: String,
) {
    val protocolExtra: ProtocolExtraItem by lazy {
        try {
            // ProtocolExtraItem.fromJson(protoExtraRaw)
            Json.decodeFromString<ProtocolExtraItem>(protoExtraRaw)
        } catch (e: Exception) {
            ProtocolExtraItem()
        }
    }
    val transportExtra: TransportExtraItem by lazy {
        try {
            // TransportExtraItem.fromJson(transportExtraRaw)
            Json.decodeFromString<TransportExtraItem>(transportExtraRaw)
        } catch (e: Exception) {
            TransportExtraItem()
        }
    }
}