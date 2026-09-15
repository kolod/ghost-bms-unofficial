package ua.ztr.bmsble

/**
 * Останній відомий стан BMS, зібраний з усіх типів пакетів, що надійшли дотепер.
 * BMS надсилає різні "сторінки" по черзі, тому одразу після підключення частина
 * полів буде null, доки не прийде відповідний пакет хоча б раз.
 */
data class BmsState(
    val totalVoltage: Double? = null,
    val current: Double? = null,
    val ratedCapacityAh: Double? = null,
    val cellCount: Int? = null,
    val temperatureC: Double? = null,
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
    val cumulativeDischargeCapacityAh: Double? = null,
    val cumulativeCycles: Double? = null,
    val triggeringCellNumber: Int? = null,
    val settings: BmsSettings? = null,
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
                totalVoltage = frame.totalVoltage,
                current = frame.current,
                ratedCapacityAh = frame.ratedCapacityAh,
                cellCount = frame.cellCount,
                temperatureC = frame.temperatureC,
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
                val rated = current.ratedCapacityAh
                val cycles = if (rated != null && rated > 0) frame.cumulativeDischargeCapacityAh / rated else null
                current.copy(
                    balanceStartVoltage = frame.balanceStartVoltage,
                    minCellVoltage = frame.minCellVoltage,
                    maxCellVoltage = frame.maxCellVoltage,
                    balanceBaselineVoltage = frame.balanceBaselineVoltage,
                    cumulativeDischargeCapacityAh = frame.cumulativeDischargeCapacityAh,
                    cumulativeCycles = cycles,
                    lastUpdated = now,
                )
            }

            is BmsFrame.ProtectionTriggerCell -> current.copy(
                triggeringCellNumber = frame.cellNumber,
                lastUpdated = now,
            )

            is BmsFrame.Settings -> current.copy(
                settings = frame.data,
                lastUpdated = now,
            )

            is BmsFrame.Unknown -> current
        }
}
