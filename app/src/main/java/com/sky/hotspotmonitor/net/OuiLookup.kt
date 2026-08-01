package com.sky.hotspotmonitor.net

/**
 * Lookup fabricant via préfixe MAC (OUI), 100% local.
 * V1 : mini-table des cas courants. La table OUI complète (~30k entrées) viendra en V1.1.
 */
object OuiLookup {
    private val prefixes = mapOf(
        "B8:27:EB" to "Raspberry Pi Foundation",
        "DC:A6:32" to "Raspberry Pi Trading",
        "E4:5F:01" to "Raspberry Pi Trading",
        "28:CD:C1" to "Raspberry Pi Trading",
        "DC:A4:CA" to "Apple",
        "F0:18:98" to "Apple",
        "A4:83:E7" to "Apple",
        "3C:5A:B4" to "Google",
        "F4:F5:E8" to "Google",
        "00:1A:11" to "Google",
        "FC:DB:B3" to "Samsung",
        "00:12:FB" to "Samsung",
        "50:8F:4C" to "Xiaomi",
        "64:09:80" to "Xiaomi",
        "00:E0:4C" to "Realtek",
        "DC:A6:32" to "Raspberry Pi Trading",
    )

    fun vendorFor(mac: String?): String? {
        if (mac == null) return null
        val key = mac.uppercase().split(":").take(3).joinToString(":")
        return prefixes[key]
    }
}
