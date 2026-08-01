package com.sky.hotspotmonitor.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky.hotspotmonitor.MainViewModel
import com.sky.hotspotmonitor.net.ConnectedDevice
import com.sky.hotspotmonitor.net.HotspotNetworkInfo
import com.sky.hotspotmonitor.net.ScanState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContextSafe()
    val net = state.network

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HotspotIP Monitor", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            if (net.active) {
                val scanning = state.scan is ScanState.Scanning
                ExtendedFloatingActionButton(
                    onClick = { if (!scanning) vm.refreshNetwork(autoScan = true) },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    text = { Text(if (scanning) "Scan en cours…" else "Rafraîchir") },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { NetworkCard(net, context) }

            if (!net.active) {
                item { InactiveCard(context) }
            } else {
                item { ScanHeader(state.scan) }
                val devices = (state.scan as? ScanState.Done)?.devices ?: emptyList()
                items(devices, key = { it.ip }) { d -> DeviceRow(d, context) }
                if (state.scan is ScanState.Done && devices.isEmpty()) {
                    item { EmptyDevices() }
                }
            }
        }
    }
}

@Composable
private fun NetworkCard(net: HotspotNetworkInfo, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(active = net.active)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (net.active) "Hotspot actif" else "Hotspot inactif",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (net.active) {
                Spacer(Modifier.size(8.dp))
                InfoLine("Passerelle (ce tél.)", net.gatewayIp ?: "—", context, copyable = true)
                InfoLine("Masque", net.netmask ?: "—", context)
                InfoLine("Réseau (CIDR)", net.cidr ?: "—", context, copyable = true)
                InfoLine(
                    "Plage utilisable",
                    "${net.hostRangeFirst} – ${net.hostRangeLast}",
                    context,
                )
                InfoLine("Interface", net.interfaceName ?: "—", context)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, context: Context, copyable: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = copyable) { copyToClipboard(context, label, value) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ScanHeader(scan: ScanState) {
    when (scan) {
        is ScanState.Idle -> {
            Text(
                "Appuie sur Rafraîchir pour scanner les appareils connectés.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
        is ScanState.Scanning -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Scan : ${scan.done} / ${scan.total} adresses",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                val progress = if (scan.total > 0) scan.done.toFloat() / scan.total else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        is ScanState.Done -> {
            Text(
                "${scan.devices.size} appareil(s) détecté(s) en ${scan.durationMs} ms",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun DeviceRow(d: ConnectedDevice, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { copyToClipboard(context, "IP", d.ip) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    d.ip,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val sub = buildString {
                    append(d.mac ?: "MAC indisponible")
                    if (d.vendor != null) append("  •  ${d.vendor}")
                }
                Text(
                    sub,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            val tag = if (d.isSelf) "Ce téléphone" else d.reachableVia
            Text(
                tag,
                fontSize = 11.sp,
                color = if (d.isSelf) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun EmptyDevices() {
    Text(
        "Aucun appareil détecté. Certains clients en veille peuvent ne pas répondre au ping.",
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun InactiveCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Active le partage de connexion (hotspot) sur ton téléphone, puis reviens ici.",
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(onClick = { openTetherSettings(context) }) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ouvrir les paramètres")
            }
        }
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        Modifier
            .size(12.dp)
            .background(
                color = if (active) Color(0xFF34D399) else Color(0xFFEF4444),
                shape = CircleShape,
            )
    )
}

private fun openTetherSettings(context: Context) {
    val tryIntents = listOf(
        Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in tryIntents) {
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copié : $text", Toast.LENGTH_SHORT).show()
}

@Composable
private fun LocalContextSafe(): Context =
    androidx.compose.ui.platform.LocalContext.current
