package com.clearpath.xray_compose.data

import kotlinx.serialization.Serializable

@Serializable
data class ProtocolExtraItem(
    val uot: String? = null,
    val congestionControl: String? = null,
    // VMess
    val alterId: String? = null,
    val vmessSecurity: String? = null,
    // VLESS
    val flow: String? = null,
    val vlessEncryption: String? = null,
    // shadowsocks
    val ssMethod: String? = null,
    // wireguard
    val wgPublicKey: String? = null,
    // use password instead
    // val wgPrivateKey: String? = null,
    val wgPreSharedKey: String? = null,
    val wgInterfaceAddress: String? = null,
    val wgReserveds: String? = null,
    val wgMtu: String? = null,
    // hysteria2
    val salamanderPass: String? = null,
    val upMbps: String? = null,
    val downMbps: String? = null,
    val ports: String? = null,
    val hopInterval: String? = null,
    // group profile
    val groupType: String? = null,
    val childItemIds: String? = null,
    val childSubIds: String? = null,
    val filter: String? = null,
    val multipleLoad: String? = null,
)