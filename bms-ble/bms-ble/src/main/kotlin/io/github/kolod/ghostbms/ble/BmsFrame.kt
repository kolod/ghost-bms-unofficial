package io.github.kolod.ghostbms.ble

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
        /** Налаштований стан каналу за замовчуванням (readout команди 0x0C) — НЕ живий стан реле. */
        val defaultChannelOn: Boolean,
        val protectionCode: ProtectionCode,
        val screenOff: Boolean,
        /** Живий стан реле розряду (MOSFET), offset 9-10 біт 0. Підтверджено в C2.java ("放电mos"). */
        val dischargeMosOn: Boolean,
        /** Живий стан реле заряду (MOSFET), offset 9-10 біт 1. Підтверджено в C2.java ("充电mos"). */
        val chargeMosOn: Boolean,
        /** Температура MOSFET, °C (зі знаком, offset 17 — 1=від'ємне). Offset 15-16. */
        val mosTemperatureC: Double,
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
     * pages 0x02 і 0x0A (10) — ще один, ОКРЕМИЙ набір температур модулів (номер
     * датчика 1-based → °C), знайдений в `C2.java`. НЕ дублює [ModuleTemperatures]
     * (page 0x12) — на реальному пристрої дає інші, реальні значення, тоді як
     * page 0x12 повертала самі нулі (датчики цього банку фізично не підключені).
     *
     * Підтверджено (реально є на пристрої): по 4 датчики з кожної сторінки —
     * `startProbe`=1..4 (0x02) і 5..8 (0x0A).
     *
     * TODO: BMS підтримує до 16 датчиків. Байтова структура кадру ідентична
     * сторінці 0x12 (8 послідовних пар замість 4) — можливо, обидві сторінки
     * насправді несуть по 8 датчиків кожна (0x02→1-8, 0x0A→9-16, разом усі 16),
     * а штатний застосунок сам показує лише половину. Не реалізовано й не
     * перевірено — немає датчиків для цих слотів на реальному пристрої.
     * Деталі й точні offset-и — див. `BmsFrameParser.parseAuxModuleTemperatures`.
     */
    data class AuxModuleTemperatures(
        val probes: Map<Int, Double>,
    ) : BmsFrame()

    /**
     * pages 0x13-0x1D (19-29) — напруги комірок, банк A. Кожен кадр несе підмножину
     * комірок (номер 1-based → вольти); мапа сторінка→діапазон нелінійна, див. format.md.
     *
     * ВИПРАВЛЕНО: раніше вважалось, що сторінки C2 (3-7, 11-15) і C4 (19-23, 25-29)
     * — це ДУБЛІКАТ одних і тих самих номерів комірок під різними pageType (звідси й
     * стара mergeNonZero-логіка проти "блимання"). Це було невірно: однакові номери
     * (наприклад, "комірка 1" на сторінці 3 і "комірка 1" на сторінці 19) насправді
     * позначають РІЗНІ фізичні комірки з різних банків — комірка 1 банку A і комірка 1
     * банку B. 192-комірковий пакет = 2 банки по 96. Банк B — [AuxCellVoltages].
     */
    data class CellVoltages(
        val cells: Map<Int, Double>,
    ) : BmsFrame()

    /**
     * pages 0x03-0x07, 0x0B-0x0F (3-7, 11-15) — напруги комірок, банк B (нумерація
     * вікна C2.java). Той самий формат кадру, що й [CellVoltages], але окремий
     * фізичний банк комірок — номери НЕ перетинаються з банком A попри однакові
     * номери-мітки (комірка 1 банку A ≠ комірка 1 банку B).
     *
     * TODO: сторінка 8 (C2), за аналогією зі сторінкою 24 для банку A, імовірно несе
     * "залишкові" напруги комірок 46-48/94-96 банку B — не реалізовано, не підтверджено.
     */
    data class AuxCellVoltages(
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
