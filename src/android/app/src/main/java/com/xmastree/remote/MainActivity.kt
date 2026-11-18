package com.xmastree.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xmastree.remote.ble.AndroidBleClient
import com.xmastree.remote.ble.BleConnectionState
import com.xmastree.remote.data.PresetRepository
import com.xmastree.remote.ui.TreeRemoteViewModel
import com.xmastree.remote.ui.screens.ControlScreen
import com.xmastree.remote.ui.screens.PresetsScreen
import com.xmastree.remote.ui.screens.ScanScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bleClient = AndroidBleClient(applicationContext)
        val presetRepo = PresetRepository(applicationContext)

        setContent {
            MaterialTheme {
                val vm: TreeRemoteViewModel = viewModel(
                    factory = TreeRemoteViewModel.Factory(bleClient, presetRepo)
                )
                XmasTreeRemoteApp(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XmasTreeRemoteApp(viewModel: TreeRemoteViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Xmas Tree Remote", style = MaterialTheme.typography.titleLarge)
                        val telemetry = uiState.telemetry
                        val subtitle = when {
                            telemetry != null ->
                                "Connected • Up ${telemetry.systemUptimeSeconds}s • Bright ${telemetry.currentBrightness}% • Vol ${telemetry.currentVolume}%"
                            uiState.connectionState == BleConnectionState.CONNECTED -> "Connected"
                            else -> "BLE: ${uiState.connectionState.name}"
                        }
                        Text(subtitle, style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.BluetoothSearching, contentDescription = "Scan") },
                    label = { Text("Scan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Control") },
                    label = { Text("Control") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Presets") },
                    label = { Text("Presets") }
                )
            }
        }
    ) { innerPadding ->
        val mod = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (selectedTab) {
            0 -> ScanScreen(
                uiState = uiState,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
                modifier = mod
            )
            1 -> ControlScreen(
                uiState = uiState,
                onSelectPattern = viewModel::selectPattern,
                onColorSelected = viewModel::setColor,
                onBrightnessChange = viewModel::setBrightness,
                onSpeedChange = viewModel::setSpeed,
                onSelectMelody = viewModel::selectMelody,
                onPlay = viewModel::playAudio,
                onPause = viewModel::pauseAudio,
                onStop = viewModel::stopAudio,
                onVolumeChange = viewModel::setVolume,
                onTempoChange = viewModel::setTempo,
                modifier = mod
            )
            else -> PresetsScreen(
                uiState = uiState,
                onSavePreset = viewModel::saveCurrentAsPreset,
                onApplyPreset = viewModel::applyPreset,
                onDeletePreset = viewModel::deletePreset,
                modifier = mod
            )
        }
    }
}
