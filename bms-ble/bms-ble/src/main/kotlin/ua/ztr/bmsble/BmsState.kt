package ua.ztr.bmsble

/**
 * Останній відомий стан BMS, зібраний з усіх типів пакетів, що надійшли дотепер.
 * BMS надсилає різні "сторінки" по черзі, тому одразу після підключення частина
 * полів буде null, доки не прийде відповідний пакет хоча б раз.
 */
data class BmsState(
    /** Сторінка 1, offset 13-14 — ПІДТВЕРДЖЕНО на пристрої: жива напруга батареї, В. */
    val liveVoltage: Double? = null,
    /** Сторінка 1, offset 15-16 — ПІДТВЕРДЖЕНО на пристрої: живий струм батареї, А. */
    val liveCurrent: Double? = null,
    /** Сторінка 1, offset 17-18 — жива потужність батареї, кВт. */
    val livePowerKw: Double? = null,
    /** Readout налаштування 0x01 — напруга відсічки розряду, В/комірку (НЕ жива напруга батареї). */
    val dischargeCutoffVoltagePerCell: Double? = null,
    /** Readout налаштування 0x02 — номінальний струм реле/MOSFET захисту, А (НЕ живий струм навантаження). */
    val dischargeProtectionCurrent: Double? = null,
    /** Readout налаштування 0x03 — максимальна (номінальна) ємність батареї, Аг. */
    val maxBatteryCapacityAh: Double? = null,
    /** Readout налаштування 0x04 — налаштована кількість комірок. */
    val cellCount: Int? = null,
    /** Readout налаштування 0x05 — напруга відсічки заряду, В/комірку. */
    val chargeCutoffVoltagePerCell: Double? = null,
    /** Readout налаштування 0x06 — уставка макс. температури (ймовірно балансувальних модулів). */
    val highTemperatureProtectionThreshold: Double? = null,
    val usedCapacityAh: Double? = null,
    val chargeRecoveryVoltage: Double? = null,
    val dischargeRecoveryVoltage: Double? = null,
    val defaultChannelOn: Boolean? = null,
    val protectionCode: ProtectionCode? = null,
    val screenOff: Boolean? = null,
    val status: BmsStatusFlags? = null,
    val balanceStartVoltage: Double? = null,
    val minCellVoltage: Double? = null,
    val maxCellVoltage: Double? = null,
    val balanceBaselineVoltage: Double? = null,
    /** Readout налаштування "низька напруга відключення хоста" (команда запису 0x0E). */
    val lowVoltageHostShutdownVoltage: Double? = null,
    /** Readout налаштування "затримка відключення хоста" (команда запису 0x0F). */
    val hostShutdownDelaySeconds: Int? = null,
    val cumulativeDischargeCapacityAh: Double? = null,
    val cumulativeCycles: Double? = null,
    val triggeringCellNumber: Int? = null,
    val settings: BmsSettings? = null,
    /** Напруги окремих комірок (номер 1-based → вольти). Комірки 97-192 протоколом не передаються. */
    val cellVoltages: Map<Int, Double> = emptyMap(),
    val moduleTemperaturesC: List<Double>? = null,
    val lastUpdated: Long = 0L,
) {
    val cellVoltageDiff: Double?
        get() = if (minCellVoltage != null && maxCellVoltage != null) maxCellVoltage - minCellVoltage else null
}

/** Комбінує чергові [BmsFrame] у сукупний [BmsState]. */
object BmsStateReducer {

    fun reduce(current: BmsState, frame: BmsFrame, now: Long = System.currentTimeMillis()): BmsState =
        when (frame) {
            is BmsFrame.BasicInfo -> current.copy(
                liveVoltage = frame.liveVoltage,
                liveCurrent = frame.liveCurrent,
                livePowerKw = frame.livePowerKw,
                dischargeCutoffVoltagePerCell = frame.dischargeCutoffVoltagePerCell,
                dischargeProtectionCurrent = frame.dischargeProtectionCurrent,
                maxBatteryCapacityAh = frame.maxBatteryCapacityAh,
                cellCount = frame.cellCount,
                chargeCutoffVoltagePerCell = frame.chargeCutoffVoltagePerCell,
                highTemperatureProtectionThreshold = frame.highTemperatureProtectionThreshold,
                lastUpdated = now,
            )

            is BmsFrame.UsedCapacity -> current.copy(
                usedCapacityAh = frame.usedCapacityAh,
                lastUpdated = now,
            )

            is BmsFrame.ProtectionStatus -> current.copy(
                chargeRecoveryVoltage = frame.chargeRecoveryVoltage,
                dischargeRecoveryVoltage = frame.dischargeRecoveryVoltage,
                defaultChannelOn = frame.defaultChannelOn,
                protectionCode = frame.protectionCode,
                screenOff = frame.screenOff,
                status = frame.status,
                lastUpdated = now,
            )

            is BmsFrame.CellStats -> {
                val rated = current.maxBatteryCapacityAh
                val cycles = if (rated != null && rated > 0) frame.cumulativeDischargeCapacityAh / rated else null
                current.copy(
                    balanceStartVoltage = frame.balanceStartVoltage,
                    minCellVoltage = frame.minCellVoltage,
                    maxCellVoltage = frame.maxCellVoltage,
                    balanceBaselineVoltage = frame.balanceBaselineVoltage,
                    lowVoltageHostShutdownVoltage = frame.lowVoltageHostShutdownVoltage,
                    hostShutdownDelaySeconds = frame.hostShutdownDelaySeconds,
                    cumulativeDischargeCapacityAh = frame.cumulativeDischargeCapacityAh,
                    cumulativeCycles = cycles,
                    lastUpdated = now,
                )
            }

            is BmsFrame.ProtectionTriggerCell -> current.copy(
                triggeringCellNumber = frame.cellNumber,
                cellVoltages = current.cellVoltages + frame.remainderCells,
                lastUpdated = now,
            )

            is BmsFrame.Settings -> current.copy(
                settings = frame.data,
                lastUpdated = now,
            )

            is BmsFrame.ModuleTemperatures -> current.copy(
                moduleTemperaturesC = frame.probesC,
                lastUpdated = now,
            )

            is BmsFrame.CellVoltages -> current.copy(
                cellVoltages = current.cellVoltages + frame.cells,
                lastUpdated = now,
            )

            is BmsFrame.Unknown -> current
        }
}
