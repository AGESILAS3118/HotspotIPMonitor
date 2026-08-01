package com.sky.hotspotmonitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky.hotspotmonitor.net.HotspotNetworkInfo
import com.sky.hotspotmonitor.net.HotspotScanner
import com.sky.hotspotmonitor.net.NetworkInfoProvider
import com.sky.hotspotmonitor.net.ScanState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val network: HotspotNetworkInfo = HotspotNetworkInfo(active = false),
    val scan: ScanState = ScanState.Idle,
)

class MainViewModel : ViewModel() {
    private val scanner = HotspotScanner()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Relit l'état réseau ; lance automatiquement un scan si demandé et hotspot actif. */
    fun refreshNetwork(autoScan: Boolean = false) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { NetworkInfoProvider.read() }
            _state.value = _state.value.copy(network = info)
            if (autoScan && info.active) startScan()
        }
    }

    fun startScan() {
        val info = _state.value.network
        if (!info.active) return
        if (_state.value.scan is ScanState.Scanning) return
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            _state.value = _state.value.copy(scan = ScanState.Scanning(0, info.hostCount))
            val devices = scanner.scan(info) { d, t ->
                _state.value = _state.value.copy(scan = ScanState.Scanning(d, t))
            }
            _state.value = _state.value.copy(
                scan = ScanState.Done(devices, System.currentTimeMillis() - start)
            )
        }
    }
}
