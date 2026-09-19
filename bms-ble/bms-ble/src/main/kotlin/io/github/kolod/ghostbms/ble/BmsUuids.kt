package io.github.kolod.ghostbms.ble

import java.util.UUID

/**
 * UUID сервісу/характеристики модуля JDY-18 (BLE UART клон HM-10/CC254x),
 * реконструйовано декомпіляцією штатного застосунку BMS.
 */
object BmsUuids {
    val SERVICE: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
    val CHARACTERISTIC: UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
}
