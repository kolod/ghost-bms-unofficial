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
        /**
         * Offsets 13-18 — друга трійка напруга/струм/потужність, знайдена в `C4.java`.
         * Точний стосунок до [totalVoltage]/[current] (те саме джерело чи інший вимір —
         * напр. навантаження проти акумулятора) не підтверджено.
         */
        val secondaryVoltage: Double,
        val secondaryCurrent: Double,
        val powerKw: Double,
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
        /** Readout налаштування "низька напруга відключення хоста" (команда запису 0x0E). Помірна впевненість. */
        val lowVoltageHostShutdownVoltage: Double,
        val cellCountRef: Int,
        /** Readout налаштування "затримка відключення хоста" (команда запису 0x0F). Помірна впевненість. */
        val hostShutdownDelaySeconds: Int,
        val balanceStartVoltage: Double,
        val minCellVoltage: Double,
        val maxCellVoltage: Double,
        val balanceBaselineVoltage: Double,
        /** Кумулятивна розряджена ємність; масштаб (Ah напряму чи з коефіцієнтом) не підтверджено. */
        val cumulativeDischargeCapacityAh: Double,
    ) : BmsFrame() {
        val cellVoltageDiff: Double get() = maxCellVoltage - minCellVoltage
    }

    /**
     * page 0x18 (24) — номер комірки, що спричинила спрацювання захисту, і 6
     * "залишкових" напруг комірок (46-48, 94-96), які той самий кадр несе поряд.
     */
    data class ProtectionTriggerCell(
        val cellNumber: Int,
        val remainderCells: Map<Int, Double>,
    ) : BmsFrame()

    /** page 0x1F (31) — параметри/налаштування. */
    data class Settings(
        val data: BmsSettings,
    ) : BmsFrame()

    /** page 0x12 (18) — температури 8 датчиків модулів, °C (зі знаком). */
    data class ModuleTemperatures(
        val probesC: List<Double>,
    ) : BmsFrame()

    /**
     * pages 0x13-0x1D (19-29) — напруги комірок. Кожен кадр несе підмножину комірок
     * (номер 1-based → вольти); мапа сторінка→діапазон нелінійна, див. format.md.
     * Комірки 97-192 не передаються цим протоколом (штатний застосунок теж їх не показує).
     */
    data class CellVoltages(
        val cells: Map<Int, Double>,
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
    /** Readout команди запису 0x1A. */
    val fanStartTemperatureC: Int,
    /** Readout команди запису 0x1B. */
    val heaterStartTemperatureC: Int,
    /** Readout команди запису 0x1C. Захищено паролем "770921" у штатному застосунку. */
    val canSendId: Int,
    /** Readout команди запису 0x1D. Захищено паролем "770921" у штатному застосунку. */
    val canReceiveId: Int,
)
