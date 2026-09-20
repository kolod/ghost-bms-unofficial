package io.github.kolod.ghostbms.ble

/**
 * Коди захисту з page 0x10 (offset 7-8), значення підтверджені у штатному застосунку.
 * Бібліотека навмисно не несе текстових описів — локалізацію робить UI (див.
 * `protectionLabel()` в app-модулі, що мапить кожен код на `stringResource`).
 */
enum class ProtectionCode(val code: Int) {
    NONE(0),
    OVER_CURRENT(1),
    OVER_DISCHARGE(2),
    OVER_CHARGE(3),
    OVER_TEMPERATURE(4),
    WRONG_CELL_COUNT(5),
    CHARGE_MOSFET_FAULT(6),
    DISCHARGE_MOSFET_FAULT(7),
    LOW_VOLTAGE_SHUTDOWN(8),
    CELL_VOLTAGE_DIFF(9),
    LOW_TEMPERATURE(10),
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int): ProtectionCode = entries.find { it.code == code } ?: UNKNOWN
    }
}
