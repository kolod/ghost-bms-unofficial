package ua.ztr.bmsmonitor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.ztr.bmsble.BmsConnectionState
import ua.ztr.bmsmonitor.ui.BmsMonitorTheme
import ua.ztr.bmsmonitor.ui.DashboardScreen
import ua.ztr.bmsmonitor.ui.ScanScreen

private fun requiredBluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun hasBluetoothPermissions(context: Context): Boolean =
    requiredBluetoothPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

class MainActivity : ComponentActivity() {

    private val viewModel: BmsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BmsMonitorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: BmsViewModel) {
    val context = LocalContext.current

    var permissionsGranted by remember { mutableStateOf(hasBluetoothPermissions(context)) }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* результат ігноруємо — стан адаптера перевіряємо напряму нижче */ }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        permissionsGranted = result.values.all { it }
    }

    val bluetoothManager = remember {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    val adapter = bluetoothManager.adapter

    if (!permissionsGranted) {
        PermissionRequestScreen(onRequestClick = { requestPermissionsLauncher.launch(requiredBluetoothPermissions()) })
        return
    }

    if (adapter == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Цей пристрій не підтримує Bluetooth.")
        }
        return
    }

    if (!adapter.isEnabled) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Bluetooth вимкнено.")
            Button(
                onClick = { enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("Увімкнути Bluetooth")
            }
        }
        return
    }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val bmsState by viewModel.bmsState.collectAsStateWithLifecycle()
    val deviceName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanResults by viewModel.scanResults.collectAsStateWithLifecycle()

    Scaffold { padding ->
        if (connectionState == BmsConnectionState.DISCONNECTED && deviceName == null) {
            ScanScreen(
                isScanning = isScanning,
                devices = scanResults,
                onScanClick = { viewModel.startScan() },
                onDeviceClick = { viewModel.connect(it) },
                modifier = Modifier.padding(padding),
            )
        } else {
            DashboardScreen(
                deviceName = deviceName,
                connectionState = connectionState,
                state = bmsState,
                onScreenOn = { viewModel.setScreenOn(true) },
                onScreenOff = { viewModel.setScreenOn(false) },
                onDisconnect = { viewModel.disconnect() },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun PermissionRequestScreen(onRequestClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Для пошуку та підключення до BMS потрібні дозволи Bluetooth.")
        Button(onClick = onRequestClick, modifier = Modifier.padding(top = 12.dp)) {
            Text("Надати дозволи")
        }
    }
}
