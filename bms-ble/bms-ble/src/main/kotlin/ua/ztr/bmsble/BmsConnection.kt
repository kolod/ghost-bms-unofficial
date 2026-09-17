package ua.ztr.bmsble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class BmsConnectionState {
    DISCONNECTED,
    CONNECTING,
    DISCOVERING_SERVICES,
    SUBSCRIBING,
    READY,
    FAILED,
}

/**
 * Одне BLE GATT-з'єднання з BMS: підключення, підписка на нотифікації
 * характеристики FFE1, розбір вхідних пакетів і надсилання команд.
 *
 * Дані надаються асинхронно двома шляхами:
 * - [frames] — потік кожного окремого розпарсеного пакета (для журналювання/діагностики);
 * - [state]  — агрегований останній відомий стан BMS, зручний для UI.
 *
 * Клас не є потокобезпечним відносно кількох одночасних викликів [connect]/[close];
 * використовуйте один екземпляр на одне з'єднання.
 */
@SuppressLint("MissingPermission")
class BmsConnection(
    private val context: Context,
    private val device: BluetoothDevice,
) {
    private val _connectionState = MutableStateFlow(BmsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BmsConnectionState> = _connectionState.asStateFlow()

    private val _frames = MutableSharedFlow<BmsFrame>(extraBufferCapacity = 64)
    val frames: SharedFlow<BmsFrame> = _frames.asSharedFlow()

    private val _state = MutableStateFlow(BmsState())
    val state: StateFlow<BmsState> = _state.asStateFlow()

    /** Діагностичний лог сирих байтів і подій GATT для цієї сесії — див. [BmsRawLogger]. */
    val logger = BmsRawLogger(context)

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            logger.log("onConnectionStateChange status=$status newState=$newState")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BmsConnectionState.FAILED
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BmsConnectionState.DISCOVERING_SERVICES
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BmsConnectionState.DISCONNECTED
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            logger.log(
                "onServicesDiscovered status=$status services=" +
                    g.services.joinToString(",") { it.uuid.toString() },
            )
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BmsConnectionState.FAILED
                return
            }
            val characteristic = g.getService(BmsUuids.SERVICE)?.getCharacteristic(BmsUuids.CHARACTERISTIC)
            if (characteristic == null) {
                logger.log("FFE0/FFE1 not found — expected service=${BmsUuids.SERVICE} characteristic=${BmsUuids.CHARACTERISTIC}")
                _connectionState.value = BmsConnectionState.FAILED
                return
            }
            writeCharacteristic = characteristic
            _connectionState.value = BmsConnectionState.SUBSCRIBING
            g.setCharacteristicNotification(characteristic, true)

            val cccd = characteristic.getDescriptor(BmsUuids.CLIENT_CHARACTERISTIC_CONFIG)
            if (cccd == null) {
                // Немає дескриптора підписки — вважаємо, що нотифікації вже активні.
                logger.log("no CCCD descriptor on characteristic — assuming notifications already active")
                _connectionState.value = BmsConnectionState.READY
                sendCommand(BmsCommands.enterRealtimeMonitoring())
                return
            }
            @Suppress("DEPRECATION")
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            g.writeDescriptor(cccd)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            logger.log("onDescriptorWrite status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BmsConnectionState.READY
                // Штатний застосунок шле цю команду перед відкриттям екрана показників —
                // без неї BMS не починає штовхати нотифікації з даними.
                sendCommand(BmsCommands.enterRealtimeMonitoring())
            } else {
                _connectionState.value = BmsConnectionState.FAILED
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            // Викликається на API < 33; на 33+ використовується overload нижче.
            handleIncoming(characteristic.value)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleIncoming(value)
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            logger.log("onCharacteristicWrite status=$status")
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            logger.log("onMtuChanged mtu=$mtu status=$status")
        }
    }

    private fun handleIncoming(bytes: ByteArray?) {
        if (bytes == null) {
            logger.log("onCharacteristicChanged value=null")
            return
        }
        val frame = BmsFrameParser.parse(bytes)
        logger.logBytes(if (frame == null) "RX unparsed(size!=19)" else "RX ${frame::class.simpleName}", bytes)
        if (frame == null) return
        _frames.tryEmit(frame)
        _state.update { BmsStateReducer.reduce(it, frame) }
    }

    /** Ініціює підключення. Прогрес відстежуйте через [connectionState]. */
    fun connect() {
        logger.log("connect() device=${device.address} name=${device.name}")
        _connectionState.value = BmsConnectionState.CONNECTING
        // TRANSPORT_LE явно (а не TRANSPORT_AUTO за замовчуванням) — так само, як штатний
        // застосунок. На деяких чипсетах/OEM-стеках TRANSPORT_AUTO підключається й підписує
        // нотифікації без помилок, але BLE-нотифікації від периферії після цього не доходять.
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    /**
     * Надсилає готовий байтовий кадр команди (див. [BmsCommands]) у характеристику FFE1.
     * Повертає false, якщо з'єднання ще не готове ([connectionState] != READY).
     */
    fun sendCommand(command: ByteArray): Boolean {
        val g = gatt ?: return false
        val characteristic = writeCharacteristic ?: return false
        if (_connectionState.value != BmsConnectionState.READY) return false

        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        @Suppress("DEPRECATION")
        characteristic.value = command
        @Suppress("DEPRECATION")
        val accepted = g.writeCharacteristic(characteristic)
        logger.logBytes("TX accepted=$accepted", command)
        return accepted
    }

    /** Закриває GATT-з'єднання і звільняє ресурси. Після цього екземпляр непридатний для повторного [connect]. */
    fun close() {
        logger.log("close()")
        if (_connectionState.value == BmsConnectionState.READY) {
            sendCommand(BmsCommands.exitRealtimeMonitoring())
        }
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeCharacteristic = null
        _connectionState.value = BmsConnectionState.DISCONNECTED
    }
}
