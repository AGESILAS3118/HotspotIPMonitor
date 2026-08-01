package com.sky.hotspotmonitor.net

/** Infos du réseau hotspot émis par le téléphone (côté point d'accès). */
data class HotspotNetworkInfo(
    val active: Boolean,
    val interfaceName: String? = null,
    val gatewayIp: String? = null,
    val netmask: String? = null,
    val prefixLength: Int = 0,
    val cidr: String? = null,
    val hostRangeFirst: String? = null,
    val hostRangeLast: String? = null,
    val hostCount: Int = 0,
)

/** Un appareil détecté sur le sous-réseau du hotspot. */
data class ConnectedDevice(
    val ip: String,
    val mac: String? = null,
    val vendor: String? = null,
    val isSelf: Boolean = false,
    /** Comment l'appareil a répondu : "ping", "tcp:80", "arp", "local"… */
    val reachableVia: String = "",
)

/** État du scan actif. */
sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val done: Int, val total: Int) : ScanState
    data class Done(val devices: List<ConnectedDevice>, val durationMs: Long) : ScanState
}
