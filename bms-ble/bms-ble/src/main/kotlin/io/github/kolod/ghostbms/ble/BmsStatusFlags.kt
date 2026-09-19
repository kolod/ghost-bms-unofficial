package io.github.kolod.ghostbms.ble

/** Бітові прапорці статусу з page 0x10, останній байт (offset 18). */
data class BmsStatusFlags(
    /**
     * Біт 0x80 — "通道打开/通道关闭" (канал відкрито/закрито) у C2.java. Загальний стан
     * силового кола (можливо, включно з пусковим реле) — окремо від [BmsFrame.ProtectionStatus.chargeMosOn]/
     * [BmsFrame.ProtectionStatus.dischargeMosOn], які саме заряд/розряд MOSFET. Точний
     * фізичний сенс (чи це саме пускове реле) не підтверджено.
     */
    val channelOpen: Boolean,
    val isCharging: Boolean,
    val isBalancing: Boolean,
    val alarmLowVoltage: Boolean,
    val alarmOverCurrent: Boolean,
    val alarmWrongCellCount: Boolean,
    val alarmHighVoltage: Boolean,
    val alarmHighTemperature: Boolean,
) {
    companion object {
        fun fromByte(byte: Int): BmsStatusFlags = BmsStatusFlags(
            channelOpen = byte and 0x80 != 0,
            isCharging = byte and 0x40 != 0,
            isBalancing = byte and 0x20 != 0,
            alarmLowVoltage = byte and 0x10 != 0,
            alarmOverCurrent = byte and 0x08 != 0,
            alarmWrongCellCount = byte and 0x04 != 0,
            alarmHighVoltage = byte and 0x02 != 0,
            alarmHighTemperature = byte and 0x01 != 0,
        )
    }
}
