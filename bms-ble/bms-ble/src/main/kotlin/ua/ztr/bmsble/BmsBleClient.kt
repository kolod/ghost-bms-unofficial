package ua.ztr.bmsble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Точка входу в бібліотеку.
 *
 * Приклад використання:
 * ```
 * val client = BmsBleClient(context)
 *
 * // 1. Пошук пристрою
 * client.scan(nameFilter = "JDY").collect { device ->
 *     // показати користувачу / вибрати перший
 * }
 *
 * // 2. Підключення й отримання даних
 * val connection = client.connect(device)
 * launch {
 *     connection.state.collect { state ->
 *         println("Напруга: ${state.totalVoltage} В, SOC-споріднені поля: ${state.usedCapacityAh} Ah")
 *     }
 * }
 *
 * // 3. Надсилання команди
 * connection.sendCommand(BmsCommands.screenOn())
 *
 * // 4. Завершення роботи
 * connection.close()
 * ```
 *
 * Виклик відповідає за отримання рантайм-дозволів Bluetooth/Location перед
 * викликом [scan] чи [connect] — бібліотека їх не запитує.
 */
class BmsBleClient(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val scanner = BmsScanner(bluetoothManager.adapter)

    fun scan(
        nameFilter: String? = null,
        timeoutMs: Long = 10_000,
        useServiceUuidFilter: Boolean = false,
    ): Flow<BluetoothDevice> = scanner.scan(nameFilter, timeoutMs, useServiceUuidFilter)

    /** Створює й одразу ініціює нове GATT-з'єднання з [device]. */
    fun connect(device: BluetoothDevice): BmsConnection {
        val connection = BmsConnection(context.applicationContext, device)
        connection.connect()
        return connection
    }
}
