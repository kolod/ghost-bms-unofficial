package io.github.kolod.ghostbms.ble

/** Коди захисту з page 0x10 (offset 7-8), значення підтверджені у штатному застосунку. */
enum class ProtectionCode(val code: Int, val description: String) {
    NONE(0, "Немає"),
    OVER_CURRENT(1, "Перевищення струму"),
    OVER_DISCHARGE(2, "Перерозряд"),
    OVER_CHARGE(3, "Перезаряд"),
    OVER_TEMPERATURE(4, "Перегрів"),
    WRONG_CELL_COUNT(5, "Невірна кількість комірок"),
    CHARGE_MOSFET_FAULT(6, "Несправність MOSFET заряду"),
    DISCHARGE_MOSFET_FAULT(7, "Несправність MOSFET розряду"),
    LOW_VOLTAGE_SHUTDOWN(8, "Просідання живлення хоста"),
    CELL_VOLTAGE_DIFF(9, "Захист по різниці напруг комірок"),
    LOW_TEMPERATURE(10, "Захист від низької температури"),
    UNKNOWN(-1, "Невідомий код");

    companion object {
        fun fromCode(code: Int): ProtectionCode = entries.find { it.code == code } ?: UNKNOWN
    }
}
