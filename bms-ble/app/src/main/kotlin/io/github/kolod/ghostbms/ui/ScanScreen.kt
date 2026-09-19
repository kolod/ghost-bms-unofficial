package io.github.kolod.ghostbms.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kolod.ghostbms.R
import io.github.kolod.ghostbms.ScannedDevice

@Composable
fun ScanScreen(
    isScanning: Boolean,
    devices: List<ScannedDevice>,
    onScanClick: () -> Unit,
    onDeviceClick: (ScannedDevice) -> Unit,
    onDemoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.scan_screen_title), style = MaterialTheme.typography.titleLarge)
            Button(onClick = onScanClick, enabled = !isScanning) {
                Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                Text("  " + stringResource(if (isScanning) R.string.scan_scanning_label else R.string.scan_button))
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
                stringResource(R.string.scan_hint),
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        OutlinedButton(onClick = onDemoClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.demo_mode_button))
        }
    }
}
