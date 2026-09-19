package io.github.kolod.ghostbms.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class BmsScanException(message: String) : Exception(message)

/**
 * Пошук BLE-пристроїв BMS. Викликач відповідає за наявність дозволів
 * (BLUETOOTH_SCAN на Android 12+, ACCESS_FINE_LOCATION на старіших версіях)
 * і за увімкнений Bluetooth — методи тут не запитують дозволи самостійно.
 */
@SuppressLint("MissingPermission")
class BmsScanner(private val bluetoothAdapter: BluetoothAdapter) {

    /**
     * Холодний [Flow], що емітить кожен унікальний знайдений пристрій один раз
     * і завершується сам після [timeoutMs] мс (або при скасуванні колектора).
     *
     * @param nameFilter підрядок імені пристрою (наприклад "JDY"), null = без фільтра за іменем.
     * @param useServiceUuidFilter true = фільтрувати за service UUID FFE0 у пакеті реклами.
     *   Багато дешевих клонів JDY-18 не рекламують service UUID до підключення,
     *   тому за замовчуванням фільтр вимкнено.
     */
    fun scan(
        nameFilter: String? = null,
        timeoutMs: Long = 10_000,
        useServiceUuidFilter: Boolean = false,
    ): Flow<BluetoothDevice> = callbackFlow {
        val scanner = bluetoothAdapter.bluetoothLeScanner
            ?: throw BmsScanException("BLE-сканер недоступний (Bluetooth вимкнено?)")

        val seenAddresses = mutableSetOf<String>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: result.scanRecord?.deviceName
                if (nameFilter != null && (name == null || !name.contains(nameFilter, ignoreCase = true))) {
                    return
                }
                if (seenAddresses.add(device.address)) {
                    trySend(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(BmsScanException("BLE scan failed, код=$errorCode"))
            }
        }

        val filters = if (useServiceUuidFilter) {
            listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BmsUuids.SERVICE)).build())
        } else {
            emptyList()
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(filters, settings, callback)

        val timeoutJob = launch {
            delay(timeoutMs)
            close()
        }

        awaitClose {
            timeoutJob.cancel()
            scanner.stopScan(callback)
        }
    }
}
