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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ua.ztr.bmsble.BmsConnectionState
import ua.ztr.bmsmonitor.ui.BmsMonitorTheme
import ua.ztr.bmsmonitor.ui.CellVoltagesScreen
import ua.ztr.bmsmonitor.ui.DashboardScreen
import ua.ztr.bmsmonitor.ui.ScanScreen
import ua.ztr.bmsmonitor.ui.SettingsScreen

private enum class AppScreen(val title: String) {
    Dashboard("Дашборд"),
    Settings("Налаштування"),
    CellVoltages("Напруги комірок"),
}

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

private fun shareLogFile(context: Context, file: java.io.File) {
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Поділитися логом BMS"))
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
    val logFile by viewModel.logFile.collectAsStateWithLifecycle()
    val logLines by viewModel.logLines.collectAsStateWithLifecycle()

    var screen by remember { mutableStateOf(AppScreen.Dashboard) }
    BackHandler(enabled = screen != AppScreen.Dashboard) { screen = AppScreen.Dashboard }
    LaunchedEffect(connectionState, deviceName) {
        if (connectionState == BmsConnectionState.DISCONNECTED && deviceName == null) {
            screen = AppScreen.Dashboard
        }
    }

    if (connectionState == BmsConnectionState.DISCONNECTED && deviceName == null) {
        Scaffold { padding ->
            ScanScreen(
                isScanning = isScanning,
                devices = scanResults,
                onScanClick = { viewModel.startScan() },
                onDeviceClick = { viewModel.connect(it) },
                onDemoClick = if (BuildConfig.DEBUG) ({ viewModel.connectDemo() }) else null,
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text(AppScreen.Dashboard.title) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    selected = screen == AppScreen.Dashboard,
                    onClick = { screen = AppScreen.Dashboard; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                NavigationDrawerItem(
                    label = { Text(AppScreen.Settings.title) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = screen == AppScreen.Settings,
                    onClick = { screen = AppScreen.Settings; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                NavigationDrawerItem(
                    label = { Text(AppScreen.CellVoltages.title) },
                    icon = { Icon(Icons.Default.BatteryFull, contentDescription = null) },
                    selected = screen == AppScreen.CellVoltages,
                    onClick = { screen = AppScreen.CellVoltages; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                AppTopBar(title = screen.title, onMenuClick = { scope.launch { drawerState.open() } })
            },
        ) { padding ->
            when (screen) {
                AppScreen.Dashboard -> DashboardScreen(
                    deviceName = deviceName,
                    connectionState = connectionState,
                    state = bmsState,
                    logLines = logLines,
                    onScreenOn = { viewModel.setScreenOn(true) },
                    onScreenOff = { viewModel.setScreenOn(false) },
                    onChannelOpen = { viewModel.setChannel(it) },
                    onAutoBalance = { viewModel.setAutoBalance(it) },
                    onDisconnect = { viewModel.disconnect() },
                    onShareLog = logFile?.let { file -> { shareLogFile(context, file) } },
                    modifier = Modifier.padding(padding),
                )

                AppScreen.Settings -> SettingsScreen(
                    state = bmsState,
                    onDischargeCutoffVoltage = { viewModel.setDischargeCutoffVoltage(it) },
                    onDischargeProtectionCurrent = { viewModel.setDischargeProtectionCurrent(it) },
                    onMaxBatteryCapacityAh = { viewModel.setMaxBatteryCapacityAh(it) },
                    onTotalCellCount = { viewModel.setTotalCellCount(it) },
                    onChargeCutoffVoltage = { viewModel.setChargeCutoffVoltage(it) },
                    onHighTemperatureProtection = { viewModel.setHighTemperatureProtection(it) },
                    onChargeRecoveryVoltage = { viewModel.setChargeRecoveryVoltage(it) },
                    onDischargeRecoveryVoltage = { viewModel.setDischargeRecoveryVoltage(it) },
                    onDefaultChannelState = { viewModel.setDefaultChannelState(it) },
                    onLowVoltageHostShutdown = { viewModel.setLowVoltageHostShutdown(it) },
                    onHostPowerOffDelaySec = { viewModel.setHostPowerOffDelaySec(it) },
                    onChargeBalanceVoltage = { viewModel.setChargeBalanceVoltage(it) },
                    onUsedCapacityAh = { viewModel.setUsedCapacityAh(it) },
                    onAutoResetCapacity = { viewModel.setAutoResetCapacity(it) },
                    onPreChargeDelaySec = { viewModel.setPreChargeDelaySec(it) },
                    onCellVoltageDiffThreshold = { viewModel.setCellVoltageDiffThreshold(it) },
                    onLowTemperatureThreshold = { viewModel.setLowTemperatureThreshold(it) },
                    onCurrentSensorType = { viewModel.setCurrentSensorType(it) },
                    onFanStartTemperature = { viewModel.setFanStartTemperature(it) },
                    onHeaterStartTemperature = { viewModel.setHeaterStartTemperature(it) },
                    onCanSendId = { viewModel.setCanSendId(it) },
                    onCanReceiveId = { viewModel.setCanReceiveId(it) },
                    onClearAction9 = { viewModel.clearAction9() },
                    onResetDischargeCapacity = { viewModel.resetDischargeCapacity() },
                    onClearCycleCounter = { viewModel.clearCycleCounter() },
                    modifier = Modifier.padding(padding),
                )

                AppScreen.CellVoltages -> CellVoltagesScreen(
                    state = bmsState,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: String, onMenuClick: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Меню")
            }
        },
    )
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
