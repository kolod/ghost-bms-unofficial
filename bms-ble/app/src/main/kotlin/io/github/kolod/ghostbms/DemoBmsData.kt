package io.github.kolod.ghostbms

import kotlin.math.ceil
import kotlin.math.round
import kotlin.math.sin
import io.github.kolod.ghostbms.ble.BmsSettings
import io.github.kolod.ghostbms.ble.BmsState
import io.github.kolod.ghostbms.ble.BmsStatusFlags
import io.github.kolod.ghostbms.ble.ProtectionCode

/**
 * Генерує правдоподібний (плавно змінюваний) стан BMS без реального BLE-з'єднання —
 * для демо-режиму ([BmsViewModel.connectDemo]). [tick] — лічильник кроків демо-циклу,
 * зростає щосекунди, керує плавною варіацією показників. [cellCount] — поточна
 * (змінювана через Налаштування) загальна кількість комірок демо-пакету.
 */
internal object DemoBmsData {

    const val DEFAULT_CELL_COUNT = 16
    const val MIN_CELL_COUNT = 2
    const val MAX_CELL_COUNT = 192

    /** Максимум комірок в одному банку, як у реальному протоколі (CellVoltagesScreen). */
    private const val MAX_CELLS_PER_BANK = 96

    /** Один температурний модуль обслуговує максимум 12 комірок (CellVoltagesScreen). */
    private const val CELLS_PER_TEMP_MODULE = 12
    private const val MAX_TEMP_MODULES = 16

    private const val BASE_CELL_VOLTAGE = 3.30

    fun state(tick: Int, cellCount: Int): BmsState {
        val phase = tick * 0.15
        val perBankCellCount = (cellCount / 2).coerceIn(1, MAX_CELLS_PER_BANK)
        val moduleCount = ceil(cellCount / CELLS_PER_TEMP_MODULE.toDouble()).toInt().coerceIn(1, MAX_TEMP_MODULES)

        // Банк A і банк B — різні фізичні комірки з тими самими номерами-мітками,
        // тож демо навмисно генерує для них трохи різні значення (не однакові).
        val cellVoltages = (1..perBankCellCount).associateWith { i ->
            (BASE_CELL_VOLTAGE + 0.03 * sin(phase + i * 0.4) + (i % 4) * 0.004).round(3)
        }
        val auxCellVoltages = (1..perBankCellCount).associateWith { i ->
            (BASE_CELL_VOLTAGE + 0.03 * sin(phase + i * 0.4 + 1.0) + (i % 3) * 0.003).round(3)
        }
        val simulatedLoadCurrent = 3.0 * sin(phase * 0.5)
        val simulatedVoltage = (cellVoltages.values.sum() + auxCellVoltages.values.sum()).round(2)
        val moduleTemps = (1..moduleCount).associateWith { (24.0 + 2.0 * sin(phase * 0.3 + it)).round(1) }

        return BmsState(
            // Сторінка 1, offset 13-18 — гіпотеза на живу телеметрію (перевіряється на дашборді).
            liveVoltage = simulatedVoltage,
            liveCurrent = simulatedLoadCurrent.round(1),
            livePowerKw = (simulatedVoltage * simulatedLoadCurrent / 1000.0).round(2),
            // Сторінка 1, offset 1-12 — уставки захисту (НЕ жива телеметрія), статичні правдоподібні значення.
            dischargeCutoffVoltagePerCell = 2.50,
            dischargeProtectionCurrent = 120.0,
            maxBatteryCapacityAh = 100.0,
            cellCount = cellCount,
            chargeCutoffVoltagePerCell = 3.65,
            highTemperatureProtectionThreshold = 60.0,
            usedCapacityAh = (12.0 + tick * 0.01).round(2),
            chargeRecoveryVoltage = 3.60,
            dischargeRecoveryVoltage = 2.90,
            defaultChannelOn = true,
            protectionCode = ProtectionCode.NONE,
            screenOff = false,
            chargeMosOn = simulatedLoadCurrent > 0,
            dischargeMosOn = simulatedLoadCurrent <= 0,
            mosTemperatureC = (28.0 + 3.0 * sin(phase * 0.25)).round(1),
            status = BmsStatusFlags(
                channelOpen = true,
                isCharging = simulatedLoadCurrent > 0,
                isBalancing = tick % 20 < 5,
                alarmLowVoltage = false,
                alarmOverCurrent = false,
                alarmWrongCellCount = false,
                alarmHighVoltage = false,
                alarmHighTemperature = false,
            ),
            balanceStartVoltage = 3.40,
            minCellVoltage = (cellVoltages.values + auxCellVoltages.values).min(),
            maxCellVoltage = (cellVoltages.values + auxCellVoltages.values).max(),
            balanceBaselineVoltage = 3.30,
            lowVoltageHostShutdownVoltage = 2.50,
            hostShutdownDelaySeconds = 30,
            cumulativeDischargeCapacityAh = (250.0 + tick * 0.05).round(2),
            cumulativeCycles = ((250.0 + tick * 0.05) / 100.0).round(2),
            triggeringCellNumber = null,
            settings = BmsSettings(
                preChargeDelaySec = 5,
                cellVoltageDiffThreshold = 0.30,
                autoResetCapacity = true,
                lowTemperatureThreshold = 0,
                currentSensorType = 1,
                fanStartTemperatureC = 45,
                heaterStartTemperatureC = 5,
                canSendId = 100,
                canReceiveId = 101,
            ),
            cellVoltages = cellVoltages,
            auxCellVoltages = auxCellVoltages,
            auxModuleTemperaturesC = moduleTemps,
            lastUpdated = System.currentTimeMillis(),
        )
    }

    private fun Double.round(decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return round(this * factor) / factor
    }
}
