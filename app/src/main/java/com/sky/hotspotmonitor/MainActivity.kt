package com.sky.hotspotmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky.hotspotmonitor.ui.HotspotTheme
import com.sky.hotspotmonitor.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HotspotTheme {
                val vm: MainViewModel = viewModel()
                // US-01 : dès l'ouverture, lecture réseau + scan auto si hotspot actif.
                LaunchedEffect(Unit) { vm.refreshNetwork(autoScan = true) }
                MainScreen(vm)
            }
        }
    }
}
