package io.github.kolod.ghostbms

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import io.github.kolod.ghostbms.ble.BmsConnectionState
import io.github.kolod.ghostbms.ui.BmsMonitorTheme
import io.github.kolod.ghostbms.ui.CellVoltagesScreen
import io.github.kolod.ghostbms.ui.ConnectionBanner
import io.github.kolod.ghostbms.ui.DashboardScreen
import io.github.kolod.ghostbms.ui.ScanScreen
import io.github.kolod.ghostbms.ui.SettingsScreen

private enum class AppScreen(val titleRes: Int) {
    Dashboard(R.string.nav_dashboard),
    Settings(R.string.nav_settings),
    CellVoltages(R.string.nav_cell_voltages),
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
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_log_chooser_title)))
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

    // Інкрементується при поверненні з системного діалогу "Увімкнути Bluetooth", щоб
    // форсувати повторне читання adapter.isEnabled нижче — інакше після ввімкнення
    // застосунок лишався б на екрані "Bluetooth вимкнено" до наступного recompose.
    var bluetoothRefreshTrigger by remember { mutableStateOf(0) }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { bluetoothRefreshTrigger++ }

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
        InfoScreen(
            message = stringResource(R.string.bluetooth_permission_rationale),
            actionLabel = stringResource(R.string.grant_permissions),
            onAction = { requestPermissionsLauncher.launch(requiredBluetoothPermissions()) },
        )
        return
    }

    if (adapter == null) {
        InfoScreen(message = stringResource(R.string.bluetooth_not_supported))
        return
    }

    val adapterEnabled = remember(bluetoothRefreshTrigger) { adapter.isEnabled }
    if (!adapterEnabled) {
        InfoScreen(
            message = stringResource(R.string.bluetooth_disabled),
            actionLabel = stringResource(R.string.enable_bluetooth),
            onAction = { enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
        )
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
                onDemoClick = { viewModel.connectDemo() },
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
                    label = { Text(stringResource(AppScreen.Dashboard.titleRes)) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    selected = screen == AppScreen.Dashboard,
                    onClick = { screen = AppScreen.Dashboard; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(AppScreen.Settings.titleRes)) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = screen == AppScreen.Settings,
                    onClick = { screen = AppScreen.Settings; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(AppScreen.CellVoltages.titleRes)) },
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
                ConnectionBanner(
                    deviceName = deviceName,
                    state = connectionState,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onDisconnect = { viewModel.disconnect() },
                )
            },
        ) { padding ->
            when (screen) {
                AppScreen.Dashboard -> DashboardScreen(
                    state = bmsState,
                    logLines = logLines,
                    onScreenOn = { viewModel.setScreenOn(true) },
                    onScreenOff = { viewModel.setScreenOn(false) },
                    onBatteryEnabledChange = viewModel::setBatteryEnabled,
                    onAutoBalance = viewModel::setAutoBalance,
                    onShareLog = logFile?.let { file -> { shareLogFile(context, file) } },
                    modifier = Modifier.padding(padding),
                )

                AppScreen.Settings -> SettingsScreen(
                    state = bmsState,
                    onDischargeCutoffVoltage = viewModel::setDischargeCutoffVoltage,
                    onDischargeProtectionCurrent = viewModel::setDischargeProtectionCurrent,
                    onMaxBatteryCapacityAh = viewModel::setMaxBatteryCapacityAh,
                    onTotalCellCount = viewModel::setTotalCellCount,
                    onChargeCutoffVoltage = viewModel::setChargeCutoffVoltage,
                    onHighTemperatureProtection = viewModel::setHighTemperatureProtection,
                    onChargeRecoveryVoltage = viewModel::setChargeRecoveryVoltage,
                    onDischargeRecoveryVoltage = viewModel::setDischargeRecoveryVoltage,
                    onDefaultChannelState = viewModel::setDefaultChannelState,
                    onLowVoltageHostShutdown = viewModel::setLowVoltageHostShutdown,
                    onHostPowerOffDelaySec = viewModel::setHostPowerOffDelaySec,
                    onChargeBalanceVoltage = viewModel::setChargeBalanceVoltage,
                    onUsedCapacityAh = viewModel::setUsedCapacityAh,
                    onAutoResetCapacity = viewModel::setAutoResetCapacity,
                    onPreChargeDelaySec = viewModel::setPreChargeDelaySec,
                    onCellVoltageDiffThreshold = viewModel::setCellVoltageDiffThreshold,
                    onLowTemperatureThreshold = viewModel::setLowTemperatureThreshold,
                    onCurrentSensorType = viewModel::setCurrentSensorType,
                    onFanStartTemperature = viewModel::setFanStartTemperature,
                    onHeaterStartTemperature = viewModel::setHeaterStartTemperature,
                    onCanSendId = viewModel::setCanSendId,
                    onCanReceiveId = viewModel::setCanReceiveId,
                    onClearAction9 = viewModel::clearAction9,
                    onResetDischargeCapacity = viewModel::resetDischargeCapacity,
                    onClearCycleCounter = viewModel::clearCycleCounter,
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

@Composable
private fun InfoScreen(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 12.dp)) {
                Text(actionLabel)
            }
        }
    }
}
