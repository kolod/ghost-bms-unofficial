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

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
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
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BmsConnectionState.FAILED
                return
            }
            val characteristic = g.getService(BmsUuids.SERVICE)?.getCharacteristic(BmsUuids.CHARACTERISTIC)
            if (characteristic == null) {
                _connectionState.value = BmsConnectionState.FAILED
                return
            }
            writeCharacteristic = characteristic
            _connectionState.value = BmsConnectionState.SUBSCRIBING
            g.setCharacteristicNotification(characteristic, true)

            val cccd = characteristic.getDescriptor(BmsUuids.CLIENT_CHARACTERISTIC_CONFIG)
            if (cccd == null) {
                // Немає дескриптора підписки — вважаємо, що нотифікації вже активні.
                _connectionState.value = BmsConnectionState.READY
                return
            }
            @Suppress("DEPRECATION")
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            g.writeDescriptor(cccd)
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            _connectionState.value =
                if (status == BluetoothGatt.GATT_SUCCESS) BmsConnectionState.READY else BmsConnectionState.FAILED
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
    }

    private fun handleIncoming(bytes: ByteArray?) {
        val frame = bytes?.let { BmsFrameParser.parse(it) } ?: return
        _frames.tryEmit(frame)
        _state.update { BmsStateReducer.reduce(it, frame) }
    }

    /** Ініціює підключення. Прогрес відстежуйте через [connectionState]. */
    fun connect() {
        _connectionState.value = BmsConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback)
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
        return g.writeCharacteristic(characteristic)
    }

    /** Закриває GATT-з'єднання і звільняє ресурси. Після цього екземпляр непридатний для повторного [connect]. */
    fun close() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeCharacteristic = null
        _connectionState.value = BmsConnectionState.DISCONNECTED
    }
}
