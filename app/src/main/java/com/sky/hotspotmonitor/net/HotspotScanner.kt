package com.sky.hotspotmonitor.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/**
 * Scan actif (ping sweep) du sous-réseau du hotspot.
 *
 * Stratégie de détection, par ordre :
 *  1. ICMP via [InetAddress.isReachable] (méthode principale sur un même segment L2).
 *  2. Repli : tentative de connexion TCP sur des ports courants, en parallèle.
 *  3. Lecture best-effort de /proc/net/arp pour récupérer les MAC ET attraper les
 *     hôtes silencieux au ping mais présents dans le cache ARP.
 *
 * Tout est en dégradation gracieuse : aucune étape ne fait planter l'app si elle échoue.
 */
class HotspotScanner(
    private val maxConcurrent: Int = 48,
    private val pingTimeoutMs: Int = 350,
    private val tcpTimeoutMs: Int = 250,
) {
    private val tcpPorts = listOf(80, 443, 22, 8080)

    suspend fun scan(
        info: HotspotNetworkInfo,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<ConnectedDevice> = coroutineScope {
        val firstStr = info.hostRangeFirst ?: return@coroutineScope emptyList()
        val lastStr = info.hostRangeLast ?: return@coroutineScope emptyList()
        var firstInt = NetworkInfoProvider.ipToInt(firstStr)
        var lastInt = NetworkInfoProvider.ipToInt(lastStr)

        // Sécurité : sur un sous-réseau large (< /22), on se limite au /24 de la passerelle.
        if (lastInt - firstInt + 1 > 1024) {
            val gw = NetworkInfoProvider.ipToInt(info.gatewayIp ?: firstStr)
            firstInt = (gw and (-1 shl 8)) + 1
            lastInt = (gw or 0xFF) - 1
        }

        val total = lastInt - firstInt + 1
        val done = AtomicInteger(0)
        val semaphore = Semaphore(maxConcurrent)
        val responders = java.util.Collections.synchronizedList(mutableListOf<ConnectedDevice>())
        val selfIp = info.gatewayIp

        val jobs = (firstInt..lastInt).map { ipInt ->
            launch(Dispatchers.IO) {
                semaphore.withPermit {
                    val ip = NetworkInfoProvider.intToIp(ipInt)
                    if (ip == selfIp) {
                        responders.add(ConnectedDevice(ip = ip, isSelf = true, reachableVia = "local"))
                    } else {
                        val via = probe(ip)
                        if (via != null) {
                            responders.add(ConnectedDevice(ip = ip, reachableVia = via))
                        }
                    }
                    onProgress(done.incrementAndGet(), total)
                }
            }
        }
        jobs.forEach { it.join() }

        // Fusion avec la table ARP (MAC + hôtes silencieux).
        val arp = readArpTable()
        val byIp = LinkedHashMap<String, ConnectedDevice>()
        for (d in responders) byIp[d.ip] = d
        for ((ip, mac) in arp) {
            val ipInt = runCatching { NetworkInfoProvider.ipToInt(ip) }.getOrNull() ?: continue
            if (ipInt !in firstInt..lastInt) continue
            val existing = byIp[ip]
            byIp[ip] = existing?.copy(mac = mac, vendor = OuiLookup.vendorFor(mac))
                ?: ConnectedDevice(ip = ip, mac = mac, vendor = OuiLookup.vendorFor(mac), reachableVia = "arp")
        }

        byIp.values.sortedBy { NetworkInfoProvider.ipToInt(it.ip) }
    }

    /** Renvoie la méthode ayant détecté l'hôte, ou null s'il ne répond pas. */
    private suspend fun probe(ip: String): String? = coroutineScope {
        // 1) ICMP
        runCatching {
            if (InetAddress.getByName(ip).isReachable(pingTimeoutMs)) return@coroutineScope "ping"
        }
        // 2) Repli TCP en parallèle, on prend le premier port qui s'ouvre.
        val deferreds = tcpPorts.map { port ->
            async(Dispatchers.IO) {
                runCatching {
                    Socket().use { s ->
                        s.connect(InetSocketAddress(ip, port), tcpTimeoutMs)
                        port
                    }
                }.getOrNull()
            }
        }
        val open = deferreds.awaitAll().firstOrNull { it != null }
        open?.let { "tcp:$it" }
    }

    private fun readArpTable(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        runCatching {
            BufferedReader(FileReader("/proc/net/arp")).use { br ->
                br.readLine() // entête
                var line = br.readLine()
                val macRegex = Regex("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")
                while (line != null) {
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 4) {
                        val ip = parts[0]
                        val mac = parts[3]
                        if (macRegex.matches(mac) && mac != "00:00:00:00:00:00") {
                            map[ip] = mac.uppercase()
                        }
                    }
                    line = br.readLine()
                }
            }
        }
        // En cas d'échec (Android récent bloque /proc/net/arp) : map vide, pas de crash.
        return map
    }
}
