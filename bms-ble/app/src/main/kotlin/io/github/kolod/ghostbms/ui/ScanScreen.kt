package ua.ztr.bmsmonitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.ztr.bmsmonitor.ScannedDevice

@Composable
fun ScanScreen(
    isScanning: Boolean,
    devices: List<ScannedDevice>,
    onScanClick: () -> Unit,
    onDeviceClick: (ScannedDevice) -> Unit,
    /** Не-null лише в debug-збірці — кнопка емуляції підключеного пристрою для перевірки UI. */
    onDemoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Пристрої поблизу", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onScanClick, enabled = !isScanning) {
                Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                Text(if (isScanning) "  Сканування…" else "  Сканувати")
            }
        }

        if (isScanning) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        if (devices.isEmpty() && !isScanning) {
            Text(
                "Натисніть \"Сканувати\", щоб знайти BMS. Переконайтесь, що Bluetooth увімкнено, а BMS живиться.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
            items(devices, key = { it.address }) { scanned ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onDeviceClick(scanned) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(scanned.name, style = MaterialTheme.typography.titleMedium)
                        Text(scanned.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (onDemoClick != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            OutlinedButton(onClick = onDemoClick, modifier = Modifier.fillMaxWidth()) {
                Text("🧪 Демо-режим (емуляція підключеного пристрою)")
            }
        }
    }
}
