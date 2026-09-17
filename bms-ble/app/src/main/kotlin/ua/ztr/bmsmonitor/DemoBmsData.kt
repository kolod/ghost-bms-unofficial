package ua.ztr.bmsmonitor

import kotlin.math.round
import kotlin.math.sin
import ua.ztr.bmsble.BmsSettings
import ua.ztr.bmsble.BmsState
import ua.ztr.bmsble.BmsStatusFlags
import ua.ztr.bmsble.ProtectionCode

/**
 * Генерує правдоподібний (плавно змінюваний) стан BMS без реального BLE-з'єднання —
 * лише для перевірки UI в debug-збірці ([BmsViewModel.connectDemo]). [tick] — лічильник
 * кроків демо-циклу, зростає щосекунди, керує плавною варіацією показників.
 */
internal object DemoBmsData {

    private const val CELL_COUNT = 16
    private const val BASE_CELL_VOLTAGE = 3.30

    fun state(tick: Int): BmsState {
        val phase = tick * 0.15

        val cellVoltages = (1..CELL_COUNT).associateWith { i ->
            (BASE_CELL_VOLTAGE + 0.03 * sin(phase + i * 0.4) + (i % 4) * 0.004).round(3)
        }
        val current = (3.0 * sin(phase * 0.5)).round(1)
        val moduleTemps = (1..8).map { (24.0 + 2.0 * sin(phase * 0.3 + it)).round(1) }

        return BmsState(
            totalVoltage = cellVoltages.values.sum().round(2),
            current = current,
            ratedCapacityAh = 100.0,
            cellCount = CELL_COUNT,
            temperatureC = (25.0 + sin(phase * 0.2)).round(1),
            usedCapacityAh = (12.0 + tick * 0.01).round(2),
            chargeRecoveryVoltage = 3.60,
            dischargeRecoveryVoltage = 2.90,
            defaultChannelOn = true,
            protectionCode = ProtectionCode.NONE,
            screenOff = false,
            status = BmsStatusFlags(
                isCharging = current > 0,
                isBalancing = tick % 20 < 5,
                alarmLowVoltage = false,
                alarmOverCurrent = false,
                alarmWrongCellCount = false,
                alarmHighVoltage = false,
                alarmHighTemperature = false,
            ),
            balanceStartVoltage = 3.40,
            minCellVoltage = cellVoltages.values.min(),
            maxCellVoltage = cellVoltages.values.max(),
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
            moduleTemperaturesC = moduleTemps,
            lastUpdated = System.currentTimeMillis(),
        )
    }

    private fun Double.round(decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return round(this * factor) / factor
    }
}
