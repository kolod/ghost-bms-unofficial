package ua.ztr.bmsble

/**
 * Чистий (без залежностей від Android) парсер 19-байтних BLE-нотифікацій BMS.
 * Формат реконструйовано декомпіляцією штатного застосунку — див. format.md у корені репозиторію.
 */
object BmsFrameParser {

    private const val FRAME_SIZE = 19

    /**
     * Сторінка → відсортований список номерів комірок (1-based), які вона несе, offset
     * за зростанням. Дублюючі pageType-схеми: штатний застосунок передає ті самі
     * напруги комірок ДВІЧІ під різними номерами сторінок (нумерація вікна C2 і вікна
     * C4 з декомпільованого коду) — на конкретному пристрої "живою" (не нульовою)
     * виявилась лише одна зі схем, тому парсимо обидві й зливаємо без взаємного
     * затирання нулями (див. `BmsState.mergeCellVoltages`).
     */
    private val CELL_VOLTAGE_PAGES: Map<Int, List<Int>> = buildMap {
        // "9-pair" сторінки: offset 1-2..17-18 → 9 послідовних комірок.
        // Нумерація вікна C4 (декомпільований код):
        put(19, (1..9).toList())
        put(20, (10..18).toList())
        put(21, (19..27).toList())
        put(22, (28..36).toList())
        put(23, (37..45).toList())
        // Сторінка 24 — окремий випадок, див. parseProtectionTriggerCell: той самий
        // кадр несе і номер комірки, що спричинила захист (offset 17-18), і 6
        // "залишкових" напруг комірок 46-48/94-96 (offset 1-12) — різні вікна
        // штатного застосунку читають різні байти того самого пакета.
        put(25, (49..57).toList())
        put(26, (58..66).toList())
        put(27, (67..75).toList())
        put(28, (76..84).toList())
        put(29, (85..93).toList())
        // Нумерація вікна C2 — той самий сенс (ідентичні діапазони комірок), інші
        // pageType. Підтверджено на реальному пристрої: саме ЦЯ схема виявилась
        // "живою" (19-29 стабільно нульові, 3-15 — реальні значення).
        put(3, (1..9).toList())
        put(4, (10..18).toList())
        put(5, (19..27).toList())
        put(6, (28..36).toList())
        put(7, (37..45).toList())
        put(11, (49..57).toList())
        put(12, (58..66).toList())
        put(13, (67..75).toList())
        put(14, (76..84).toList())
        put(15, (85..93).toList())
        // TODO: сторінка 8 (C2) імовірно, за аналогією зі сторінкою 24, теж несе
        // "залишкові" напруги комірок 46-48/94-96 в offset 1-12 (плюс дані ємності
        // в offset 13-18, які вже парсяться як UsedCapacity) — не реалізовано,
        // не підтверджено (на пакеті власника лише 40 комірок, тож цей діапазон
        // порожній в обох схемах і перевірити різницю зараз неможливо).
    }

    fun parse(bytes: ByteArray): BmsFrame? {
        if (bytes.size != FRAME_SIZE) return null
        val pageType = bytes[0].toInt() and 0xFF
        return when (pageType) {
            1 -> parseBasicInfo(bytes)
            2 -> parseAuxModuleTemperatures(bytes, startProbe = 1)
            8 -> parseUsedCapacity(bytes)
            10 -> parseAuxModuleTemperatures(bytes, startProbe = 5)
            16 -> parseProtectionStatus(bytes)
            17 -> parseCellStats(bytes)
            18 -> parseModuleTemperatures(bytes)
            24 -> parseProtectionTriggerCell(bytes)
            31 -> parseSettings(bytes)
            in CELL_VOLTAGE_PAGES -> parseCellVoltages(bytes, CELL_VOLTAGE_PAGES.getValue(pageType))
            else -> BmsFrame.Unknown(pageType, bytes.toList())
        }
    }

    private fun parseBasicInfo(b: ByteArray): BmsFrame.BasicInfo = BmsFrame.BasicInfo(
        dischargeCutoffVoltagePerCell = u16(b, 1) / 100.0,
        dischargeProtectionCurrent = u16(b, 3) / 10.0,
        maxBatteryCapacityAh = u16(b, 5) / 10.0,
        cellCount = u16(b, 7),
        chargeCutoffVoltagePerCell = u16(b, 9) / 100.0,
        highTemperatureProtectionThreshold = u16(b, 11) / 10.0,
        liveVoltage = u16(b, 13) / 10.0,
        liveCurrent = u16(b, 15) / 10.0,
        livePowerKw = u16(b, 17) / 10.0,
    )

    private fun parseUsedCapacity(b: ByteArray): BmsFrame.UsedCapacity = BmsFrame.UsedCapacity(
        usedCapacityAh = u16(b, 13) / 10.0,
    )

    private fun parseProtectionStatus(b: ByteArray): BmsFrame.ProtectionStatus {
        val mosBits = u16(b, 9)
        val mosTempMagnitude = u16(b, 15) / 10.0
        val mosTempNegative = (b[17].toInt() and 1) == 1
        return BmsFrame.ProtectionStatus(
            chargeRecoveryVoltage = u16(b, 1) / 100.0,
            dischargeRecoveryVoltage = u16(b, 3) / 100.0,
            defaultChannelOn = u16(b, 5) == 1,
            protectionCode = ProtectionCode.fromCode(u16(b, 7)),
            screenOff = u16(b, 11) == 1,
            dischargeMosOn = (mosBits and 1) == 1,
            chargeMosOn = (mosBits shr 1 and 1) == 1,
            mosTemperatureC = if (mosTempNegative) -mosTempMagnitude else mosTempMagnitude,
            status = BmsStatusFlags.fromByte(b[18].toInt() and 0xFF),
        )
    }

    private fun parseCellStats(b: ByteArray): BmsFrame.CellStats = BmsFrame.CellStats(
        lowVoltageHostShutdownVoltage = u16(b, 1) / 100.0,
        cellCountRef = u16(b, 3),
        hostShutdownDelaySeconds = u16(b, 5),
        balanceStartVoltage = u16(b, 7) / 100.0,
        minCellVoltage = u16(b, 9) / 1000.0,
        maxCellVoltage = u16(b, 11) / 1000.0,
        balanceBaselineVoltage = u16(b, 13) / 1000.0,
        cumulativeDischargeCapacityAh = u32(b, 15).toDouble(),
    )

    /**
     * page 24 несе одразу два непов'язаних набори даних (різні вікна штатного
     * застосунку читають різні байти того самого пакета): номер комірки, що
     * спричинила спрацювання захисту (offset 17-18), і 6 "залишкових" напруг
     * комірок 46-48/94-96 (offset 1-12, /1000.0), які не влізли в жодну з
     * регулярних "9-коміркових" сторінок 19-29.
     */
    private fun parseProtectionTriggerCell(b: ByteArray): BmsFrame.ProtectionTriggerCell =
        BmsFrame.ProtectionTriggerCell(
            cellNumber = u16(b, 17),
            remainderCells = mapOf(
                46 to u16(b, 1) / 1000.0,
                47 to u16(b, 3) / 1000.0,
                48 to u16(b, 5) / 1000.0,
                94 to u16(b, 7) / 1000.0,
                95 to u16(b, 9) / 1000.0,
                96 to u16(b, 11) / 1000.0,
            ),
        )

    private fun parseSettings(b: ByteArray): BmsFrame.Settings = BmsFrame.Settings(
        BmsSettings(
            preChargeDelaySec = u16(b, 1),
            cellVoltageDiffThreshold = u16(b, 3) / 100.0,
            autoResetCapacity = u16(b, 5) == 1,
            lowTemperatureThreshold = u16(b, 7),
            currentSensorType = u16(b, 9),
            fanStartTemperatureC = u16(b, 11),
            heaterStartTemperatureC = u16(b, 13),
            canSendId = u16(b, 15),
            canReceiveId = u16(b, 17),
        )
    )

    /** page 0x12 (18) — 8 температур модулів; знак з бітової маски `b[2]` (біт7→probe1 … біт0→probe8). */
    private fun parseModuleTemperatures(b: ByteArray): BmsFrame.ModuleTemperatures {
        val signBits = b[2].toInt() and 0xFF
        val probes = (0 until 8).map { i ->
            val magnitude = u16(b, 3 + i * 2) / 10.0
            val negative = (signBits shr (7 - i)) and 1 == 1
            if (negative) -magnitude else magnitude
        }
        return BmsFrame.ModuleTemperatures(probesC = probes)
    }

    /**
     * pages 0x02 (модулі 1-4) і 0x0A=10 (модулі 5-8) — по 4 температури, знак з
     * бітової маски `b[2]` (біти 7,5,3,1 → probe1..4 в межах кадру). Підтверджено
     * на реальному пристрої (власник має рівно 4 фізичні датчики, підключені саме
     * до цих слотів).
     *
     * TODO: offsets 5-6/9-10/13-14 (і біти знаку 6/4/2/0 байта 2) тут не читаються.
     * Структурно кадр ідентичний до сторінки 0x12 (8 послідовних пар/8 бітів знаку),
     * тож ці "дірки" МОЖУТЬ бути ще 4 датчиками (0x02→5-8, 0x0A→9-16 — разом усі 16
     * заявлених у характеристиках), а можуть бути й справді невикористаними —
     * оригінальний застосунок сам ніде їх не читає. Не реалізовано, бо перевірити
     * на реальному пристрої зараз нема змоги (немає датчиків для цих слотів).
     */
    private fun parseAuxModuleTemperatures(b: ByteArray, startProbe: Int): BmsFrame.AuxModuleTemperatures {
        val signBits = b[2].toInt() and 0xFF
        val offsets = intArrayOf(3, 7, 11, 15)
        val signBitPositions = intArrayOf(7, 5, 3, 1)
        val probes = offsets.indices.associate { i ->
            val magnitude = u16(b, offsets[i]) / 10.0
            val negative = (signBits shr signBitPositions[i]) and 1 == 1
            (startProbe + i) to (if (negative) -magnitude else magnitude)
        }
        return BmsFrame.AuxModuleTemperatures(probes)
    }

    private fun parseCellVoltages(b: ByteArray, cellNumbers: List<Int>): BmsFrame.CellVoltages {
        val cells = cellNumbers.mapIndexed { index, cellNumber ->
            cellNumber to u16(b, 1 + index * 2) / 1000.0
        }.toMap()
        return BmsFrame.CellVoltages(cells)
    }

    /** Читає 16-бітне беззнакове big-endian число з offset i, i+1. */
    private fun u16(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)

    /** Читає 32-бітне беззнакове big-endian число з offset i..i+3. */
    private fun u32(b: ByteArray, i: Int): Long =
        ((b[i].toLong() and 0xFF) shl 24) or
            ((b[i + 1].toLong() and 0xFF) shl 16) or
            ((b[i + 2].toLong() and 0xFF) shl 8) or
            (b[i + 3].toLong() and 0xFF)
}
