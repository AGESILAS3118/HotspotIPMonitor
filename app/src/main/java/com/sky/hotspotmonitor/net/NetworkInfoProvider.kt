package com.sky.hotspotmonitor.net

import java.net.Inet4Address
import java.net.InterfaceAddress
import java.net.NetworkInterface

/**
 * Détecte l'interface du point d'accès (hotspot) et en déduit IP / masque / plage.
 *
 * Note technique : côté ÉMETTEUR du hotspot, il n'existe pas d'API publique propre
 * (WifiManager.getDhcpInfo() ne vaut que côté client, getWifiApState() est une hidden
 * API restreinte). On énumère donc les interfaces réseau et on repère celle du Soft AP.
 * Cette même lecture sert à la fois à détecter que le hotspot est actif ET à récupérer
 * l'IP / le masque — sans aucune permission runtime.
 */
object NetworkInfoProvider {

    // Noms d'interface AP / tethering courants selon les OEM.
    private val apNamePrefixes = listOf("ap", "swlan", "wlan1", "softap", "rndis", "usb", "p2p")

    // Interfaces à ne jamais considérer comme un hotspot (client wifi, cellulaire, etc.).
    private val clientDenyPrefixes = listOf("wlan0", "rmnet", "ccmni", "eth", "dummy", "tun", "lo")

    fun read(): HotspotNetworkInfo {
        val candidate = findApInterface() ?: return HotspotNetworkInfo(active = false)
        val (iface, addr) = candidate
        val ip = addr.address.hostAddress ?: return HotspotNetworkInfo(active = false)
        val prefix = addr.networkPrefixLength.toInt()

        val ipInt = ipToInt(ip)
        val mask = if (prefix <= 0) 0 else (-1 shl (32 - prefix))
        val network = ipInt and mask
        val broadcast = network or mask.inv()
        val first = network + 1
        val last = broadcast - 1
        val count = (last - first + 1).coerceAtLeast(0)

        return HotspotNetworkInfo(
            active = true,
            interfaceName = iface.name,
            gatewayIp = ip,
            netmask = intToIp(mask),
            prefixLength = prefix,
            cidr = "${intToIp(network)}/$prefix",
            hostRangeFirst = intToIp(first),
            hostRangeLast = intToIp(last),
            hostCount = count,
        )
    }

    private fun findApInterface(): Pair<NetworkInterface, InterfaceAddress>? {
        val ifaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
        var fallback: Pair<NetworkInterface, InterfaceAddress>? = null
        for (iface in ifaces) {
            if (!safeUp(iface) || safeLoopback(iface)) continue
            val name = iface.name.lowercase()
            for (ia in iface.interfaceAddresses) {
                val a = ia.address
                if (a !is Inet4Address) continue
                if (!isPrivate(a.hostAddress)) continue
                // Priorité absolue : nom d'interface AP connu.
                if (apNamePrefixes.any { name.startsWith(it) }) {
                    return iface to ia
                }
                // Sinon on garde comme repli, sauf interfaces clientes connues.
                if (fallback == null && clientDenyPrefixes.none { name.startsWith(it) }) {
                    fallback = iface to ia
                }
            }
        }
        return fallback
    }

    private fun safeUp(iface: NetworkInterface) = try { iface.isUp } catch (e: Exception) { false }
    private fun safeLoopback(iface: NetworkInterface) = try { iface.isLoopback } catch (e: Exception) { true }

    private fun isPrivate(ip: String?): Boolean {
        if (ip == null) return false
        return ip.startsWith("192.168.") ||
            ip.startsWith("10.") ||
            Regex("^172\\.(1[6-9]|2[0-9]|3[01])\\.").containsMatchIn(ip)
    }

    fun ipToInt(ip: String): Int {
        val p = ip.split(".")
        return (p[0].toInt() shl 24) or (p[1].toInt() shl 16) or (p[2].toInt() shl 8) or p[3].toInt()
    }

    fun intToIp(v: Int): String =
        "${(v ushr 24) and 0xFF}.${(v ushr 16) and 0xFF}.${(v ushr 8) and 0xFF}.${v and 0xFF}"
}
