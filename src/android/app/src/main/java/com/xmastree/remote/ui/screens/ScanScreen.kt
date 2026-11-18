package com.xmastree.remote.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.xmastree.remote.ble.BleConnectionState
import com.xmastree.remote.ble.ScannedDevice
import com.xmastree.remote.ui.TreeRemoteUiState

@Composable
fun ScanScreen(
    uiState: TreeRemoteUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
        if (permissionsGranted) {
            onStartScan()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ConnectionStatusCard(
            connectionState = uiState.connectionState,
            connectedAddress = uiState.connectedDeviceAddress,
            onDisconnect = onDisconnect
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isScanning = uiState.connectionState == BleConnectionState.SCANNING
            Button(
                onClick = {
                    if (permissionsGranted) {
                        if (isScanning) onStopScan() else onStartScan()
                    } else {
                        permissionLauncher.launch(requiredPermissions)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isScanning) "Stop Scan" else "Scan for Xmas Tree")
            }
        }

        if (!permissionsGranted) {
            Text(
                text = "Bluetooth permissions are required to scan and connect to XmasTree-ESP32C6.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "Discovered Devices (${uiState.scannedDevices.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (uiState.scannedDevices.isEmpty()) {
            Text(
                text = "No devices found yet. Ensure your ESP32-C6 is powered on and advertising.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.scannedDevices, key = { it.address }) { device ->
                    ScannedDeviceRow(
                        device = device,
                        isConnected = uiState.connectedDeviceAddress == device.address &&
                            uiState.connectionState == BleConnectionState.CONNECTED,
                        onConnect = { onConnect(device.address) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: BleConnectionState,
    connectedAddress: String?,
    onDisconnect: () -> Unit
) {
    val icon = when (connectionState) {
        BleConnectionState.CONNECTED -> Icons.Default.BluetoothConnected
        BleConnectionState.SCANNING,
        BleConnectionState.CONNECTING,
        BleConnectionState.DISCOVERING_SERVICES -> Icons.Default.BluetoothSearching
        else -> Icons.Default.Bluetooth
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (connectionState == BleConnectionState.CONNECTED) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = icon, contentDescription = "Bluetooth Status")
                Column {
                    Text(
                        text = "Status: ${connectionState.name}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (connectedAddress != null) {
                        Text(
                            text = connectedAddress,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (connectionState == BleConnectionState.CONNECTED) {
                OutlinedButton(onClick = onDisconnect) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun ScannedDeviceRow(
    device: ScannedDevice,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${device.address} • ${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onConnect, enabled = !isConnected) {
                Text(if (isConnected) "Connected" else "Connect")
            }
        }
    }
}
