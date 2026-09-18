package ua.ztr.bmsble

/**
 * Розпарсений 19-байтний пакет від BMS. Тип визначається першим байтом ("page").
 * Поля без підтвердженого текстового підпису у штатному застосунку позначені як "unknownX"
 * у відповідних класах — дивись format.md у корені репозиторію.
 */
sealed class BmsFrame {

    /**
     * page 0x01 — суміш "відлуння" уставок захисту (offset 1-12, підтверджено власником
     * пристрою — це НЕ жива телеметрія) і живих показників батареї (offset 13-18,
     * підтверджено окремо). Offset 1-12: кожне поле — readout відповідної команди запису
     * з [BmsCommands] (offset у порядку зростання точно відповідає порядку команд 0x01-0x06).
     */
    data class BasicInfo(
        /** Readout команди 0x01 ([BmsCommands.dischargeCutoffVoltage]) — В/комірку. */
        val dischargeCutoffVoltagePerCell: Double,
        /**
         * Readout команди 0x02 ([BmsCommands.dischargeProtectionCurrent]) — номінальний
         * струм реле/MOSFET, що комутують батарею (А), а не поточний струм навантаження.
         */
        val dischargeProtectionCurrent: Double,
        /** Readout команди 0x03 ([BmsCommands.maxBatteryCapacityAh]) — Аг. */
        val maxBatteryCapacityAh: Double,
        /** Readout команди 0x04 ([BmsCommands.totalCellCount]) — налаштована кількість комірок. */
        val cellCount: Int,
        /** Readout команди 0x05 ([BmsCommands.chargeCutoffVoltage]) — В/комірку. */
        val chargeCutoffVoltagePerCell: Double,
        /**
         * Readout команди 0x06 ([BmsCommands.highTemperatureProtection]) — уставка макс.
         * температури; власник пристрою вважає, що, ймовірно, стосується температури
         * балансувальних модулів, але це не підтверджено остаточно.
         */
        val highTemperatureProtectionThreshold: Double,
        /** Offset 13-14 — ПІДТВЕРДЖЕНО на пристрої: жива напруга батареї, В. */
        val liveVoltage: Double,
        /** Offset 15-16 — ПІДТВЕРДЖЕНО на пристрої: живий струм батареї, А. */
        val liveCurrent: Double,
        /** Offset 17-18 — жива потужність батареї, кВт. */
        val livePowerKw: Double,
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
