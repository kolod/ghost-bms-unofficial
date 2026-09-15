package ua.ztr.bmsble

/**
 * Чистий (без залежностей від Android) парсер 19-байтних BLE-нотифікацій BMS.
 * Формат реконструйовано декомпіляцією штатного застосунку — див. format.md у корені репозиторію.
 */
object BmsFrameParser {

    private const val FRAME_SIZE = 19

    fun parse(bytes: ByteArray): BmsFrame? {
        if (bytes.size != FRAME_SIZE) return null
        val pageType = bytes[0].toInt() and 0xFF
        return when (pageType) {
            1 -> parseBasicInfo(bytes)
            8 -> parseUsedCapacity(bytes)
            16 -> parseProtectionStatus(bytes)
            17 -> parseCellStats(bytes)
            24 -> parseProtectionTriggerCell(bytes)
            31 -> parseSettings(bytes)
            else -> BmsFrame.Unknown(pageType, bytes.toList())
        }
    }

    private fun parseBasicInfo(b: ByteArray): BmsFrame.BasicInfo = BmsFrame.BasicInfo(
        totalVoltage = u16(b, 1) / 100.0,
        current = u16(b, 3) / 10.0,
        ratedCapacityAh = u16(b, 5) / 10.0,
        cellCount = u16(b, 7),
        unknownVoltage = u16(b, 9) / 100.0,
        temperatureC = u16(b, 11) / 10.0,
    )

    private fun parseUsedCapacity(b: ByteArray): BmsFrame.UsedCapacity = BmsFrame.UsedCapacity(
        usedCapacityAh = u16(b, 13) / 10.0,
    )

    private fun parseProtectionStatus(b: ByteArray): BmsFrame.ProtectionStatus = BmsFrame.ProtectionStatus(
        chargeRecoveryVoltage = u16(b, 1) / 100.0,
        dischargeRecoveryVoltage = u16(b, 3) / 100.0,
        defaultChannelOn = u16(b, 5) == 1,
        protectionCode = ProtectionCode.fromCode(u16(b, 7)),
        screenOff = u16(b, 11) == 1,
        status = BmsStatusFlags.fromByte(b[18].toInt() and 0xFF),
    )

    private fun parseCellStats(b: ByteArray): BmsFrame.CellStats = BmsFrame.CellStats(
        unknownVoltage1 = u16(b, 1) / 100.0,
        cellCountRef = u16(b, 3),
        unknownField2 = u16(b, 5),
        balanceStartVoltage = u16(b, 7) / 100.0,
        minCellVoltage = u16(b, 9) / 1000.0,
        maxCellVoltage = u16(b, 11) / 1000.0,
        balanceBaselineVoltage = u16(b, 13) / 1000.0,
        cumulativeDischargeCapacityAh = u32(b, 15).toDouble(),
    )

    private fun parseProtectionTriggerCell(b: ByteArray): BmsFrame.ProtectionTriggerCell =
        BmsFrame.ProtectionTriggerCell(cellNumber = u16(b, 17))

    private fun parseSettings(b: ByteArray): BmsFrame.Settings = BmsFrame.Settings(
        BmsSettings(
            preChargeDelaySec = u16(b, 1),
            cellVoltageDiffThreshold = u16(b, 3) / 100.0,
            autoResetCapacity = u16(b, 5) == 1,
            lowTemperatureThreshold = u16(b, 7),
            currentSensorType = u16(b, 9),
            unidentifiedFields = listOf(u16(b, 11), u16(b, 13), u16(b, 15), u16(b, 17)),
        )
    )

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
