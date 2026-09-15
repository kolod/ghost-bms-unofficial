package ua.ztr.bmsble

/**
 * Розпарсений 19-байтний пакет від BMS. Тип визначається першим байтом ("page").
 * Поля без підтвердженого текстового підпису у штатному застосунку позначені як "unknownX"
 * у відповідних класах — дивись format.md у корені репозиторію.
 */
sealed class BmsFrame {

    /** page 0x01 — основні показники. */
    data class BasicInfo(
        val totalVoltage: Double,
        /** Знак/напрям (заряд/розряд) не підтверджено — парситься як unsigned. */
        val current: Double,
        val ratedCapacityAh: Double,
        val cellCount: Int,
        val unknownVoltage: Double,
        /** Від'ємні значення не підтверджені — парситься як unsigned. */
        val temperatureC: Double,
    ) : BmsFrame()

    /** page 0x08 — використана ємність. */
    data class UsedCapacity(
        val usedCapacityAh: Double,
    ) : BmsFrame()

    /** page 0x10 — стани/захист. */
    data class ProtectionStatus(
        val chargeRecoveryVoltage: Double,
        val dischargeRecoveryVoltage: Double,
        val defaultChannelOn: Boolean,
        val protectionCode: ProtectionCode,
        val screenOff: Boolean,
        val status: BmsStatusFlags,
    ) : BmsFrame()

    /** page 0x11 — ємність/цикли/мін-макс напруга комірок. */
    data class CellStats(
        val unknownVoltage1: Double,
        val cellCountRef: Int,
        val unknownField2: Int,
        val balanceStartVoltage: Double,
        val minCellVoltage: Double,
        val maxCellVoltage: Double,
        val balanceBaselineVoltage: Double,
        /** Кумулятивна розряджена ємність; масштаб (Ah напряму чи з коефіцієнтом) не підтверджено. */
        val cumulativeDischargeCapacityAh: Double,
    ) : BmsFrame() {
        val cellVoltageDiff: Double get() = maxCellVoltage - minCellVoltage
    }

    /** page 0x18 (24) — номер комірки, що спричинила спрацювання захисту. */
    data class ProtectionTriggerCell(
        val cellNumber: Int,
    ) : BmsFrame()

    /** page 0x1F (31) — параметри/налаштування. */
    data class Settings(
        val data: BmsSettings,
    ) : BmsFrame()

    /** Пакет із невідомим типом сторінки (byte[0]) або невірною довжиною. */
    data class Unknown(
        val pageType: Int,
        val raw: List<Byte>,
    ) : BmsFrame()
}

/** page 0x1F (31) — параметри/налаштування захисту. */
data class BmsSettings(
    val preChargeDelaySec: Int,
    val cellVoltageDiffThreshold: Double,
    val autoResetCapacity: Boolean,
    val lowTemperatureThreshold: Int,
    val currentSensorType: Int,
    /** Offsets 11-18 — призначення не ідентифіковано, див. format.md п.4. */
    val unidentifiedFields: List<Int>,
)
