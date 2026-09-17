package ua.ztr.bmsmonitor

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ua.ztr.bmsble.BmsBleClient
import ua.ztr.bmsble.BmsCommands
import ua.ztr.bmsble.BmsConnection
import ua.ztr.bmsble.BmsConnectionState
import ua.ztr.bmsble.BmsState

/** UI-модель одного знайденого пристрою (адреса, а не сам [BluetoothDevice], зручніше для Compose-стану). */
data class ScannedDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice,
)

class BmsViewModel(application: Application) : AndroidViewModel(application) {

    private val client = BmsBleClient(application)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanResults = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scanResults: StateFlow<List<ScannedDevice>> = _scanResults.asStateFlow()

    private val _connectionState = MutableStateFlow(BmsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BmsConnectionState> = _connectionState.asStateFlow()

    private val _bmsState = MutableStateFlow(BmsState())
    val bmsState: StateFlow<BmsState> = _bmsState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _logFile = MutableStateFlow<File?>(null)
    /** Файл поточної/останньої сесії діагностичного логу — для кнопки "Поділитися логом". */
    val logFile: StateFlow<File?> = _logFile.asStateFlow()

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    /** Останні рядки діагностичного логу поточної сесії — для живого перегляду. */
    val logLines: StateFlow<List<String>> = _logLines.asStateFlow()

    private var scanJob: Job? = null
    private var connection: BmsConnection? = null
    private var demoJob: Job? = null

    fun startScan() {
        if (_isScanning.value) return
        _scanResults.value = emptyList()
        _isScanning.value = true
        scanJob = client.scan(timeoutMs = 12_000)
            .onEach { device ->
                val name = device.name ?: "(без імені)"
                _scanResults.update { it + ScannedDevice(name, device.address, device) }
            }
            .onCompletion { _isScanning.value = false }
            .launchIn(viewModelScope)
    }

    fun stopScan() {
        scanJob?.cancel()
        _isScanning.value = false
    }

    fun connect(target: ScannedDevice) {
        stopScan()
        stopDemo()
        connection?.close()
        _bmsState.value = BmsState()
        _connectedDeviceName.value = target.name

        val newConnection = client.connect(target.device)
        connection = newConnection
        _logFile.value = newConnection.logger.file
        _logLines.value = emptyList()

        newConnection.connectionState.onEach { _connectionState.value = it }.launchIn(viewModelScope)
        newConnection.state.onEach { _bmsState.value = it }.launchIn(viewModelScope)
        newConnection.logger.lines.onEach { _logLines.value = it }.launchIn(viewModelScope)
    }

    /**
     * Емулює підключений пристрій без реального BLE — для перевірки UI в debug-збірці
     * (кнопка "Демо-режим" на екрані сканування, показується лише коли [BuildConfig.DEBUG]).
     * Дані генеруються локально ([DemoBmsData]) і плавно змінюються, щоб імітувати живий потік.
     */
    fun connectDemo() {
        stopScan()
        connection?.close()
        connection = null
        stopDemo()

        _bmsState.value = BmsState()
        _connectedDeviceName.value = "🧪 Демо-режим"
        _logFile.value = null
        _logLines.value = listOf("Демо-режим: дані згенеровано локально, реального BLE-з'єднання немає.")

        demoJob = viewModelScope.launch {
            _connectionState.value = BmsConnectionState.CONNECTING
            delay(300)
            _connectionState.value = BmsConnectionState.DISCOVERING_SERVICES
            delay(300)
            _connectionState.value = BmsConnectionState.SUBSCRIBING
            delay(300)
            _connectionState.value = BmsConnectionState.READY

            var tick = 0
            while (isActive) {
                _bmsState.value = DemoBmsData.state(tick)
                tick++
                delay(1000)
            }
        }
    }

    private fun stopDemo() {
        demoJob?.cancel()
        demoJob = null
    }

    fun disconnect() {
        stopDemo()
        connection?.close()
        connection = null
        _connectionState.value = BmsConnectionState.DISCONNECTED
        _bmsState.value = BmsState()
        _connectedDeviceName.value = null
    }

    fun setScreenOn(on: Boolean) {
        val command = if (on) BmsCommands.screenOn() else BmsCommands.screenOff()
        connection?.sendCommand(command)
    }

    private fun send(command: ByteArray) {
        connection?.sendCommand(command)
    }

    fun setChannel(open: Boolean) = send(BmsCommands.setChannel(open))
    fun setAutoBalance(on: Boolean) = send(BmsCommands.setAutoBalance(on))
    fun clearAction9() = send(BmsCommands.clearAction9())
    fun resetDischargeCapacity() = send(BmsCommands.resetDischargeCapacity())
    fun clearCycleCounter() = send(BmsCommands.clearCycleCounter())

    fun setDischargeCutoffVoltage(volts: Double) = send(BmsCommands.dischargeCutoffVoltage(volts))
    fun setDischargeProtectionCurrent(amps: Double) = send(BmsCommands.dischargeProtectionCurrent(amps))
    fun setMaxBatteryCapacityAh(ah: Double) = send(BmsCommands.maxBatteryCapacityAh(ah))
    fun setTotalCellCount(count: Int) = send(BmsCommands.totalCellCount(count))
    fun setChargeCutoffVoltage(volts: Double) = send(BmsCommands.chargeCutoffVoltage(volts))
    fun setHighTemperatureProtection(celsius: Double) = send(BmsCommands.highTemperatureProtection(celsius))
    fun setChargeRecoveryVoltage(volts: Double) = send(BmsCommands.chargeRecoveryVoltage(volts))
    fun setDischargeRecoveryVoltage(volts: Double) = send(BmsCommands.dischargeRecoveryVoltage(volts))
    fun setDefaultChannelState(on: Boolean) = send(BmsCommands.defaultChannelState(on))
    fun setLowVoltageHostShutdown(volts: Double) = send(BmsCommands.lowVoltageHostShutdown(volts))
    fun setHostPowerOffDelaySec(seconds: Int) = send(BmsCommands.hostPowerOffDelaySec(seconds))
    fun setChargeBalanceVoltage(volts: Double) = send(BmsCommands.chargeBalanceVoltage(volts))
    fun setUsedCapacityAh(ah: Int) = send(BmsCommands.usedCapacityAh(ah))
    fun setAutoResetCapacity(on: Boolean) = send(BmsCommands.autoResetCapacity(on))
    fun setPreChargeDelaySec(seconds: Int) = send(BmsCommands.preChargeDelaySec(seconds))
    fun setCellVoltageDiffThreshold(volts: Double) = send(BmsCommands.cellVoltageDiffThreshold(volts))
    fun setLowTemperatureThreshold(celsius: Int) = send(BmsCommands.lowTemperatureThreshold(celsius))
    fun setCurrentSensorType(type: Int) = send(BmsCommands.currentSensorType(type))
    fun setFanStartTemperature(celsius: Int) = send(BmsCommands.fanStartTemperature(celsius))
    fun setHeaterStartTemperature(celsius: Int) = send(BmsCommands.heaterStartTemperature(celsius))
    fun setCanSendId(id: Int) = send(BmsCommands.canSendId(id))
    fun setCanReceiveId(id: Int) = send(BmsCommands.canReceiveId(id))

    override fun onCleared() {
        super.onCleared()
        stopDemo()
        connection?.close()
    }
}
