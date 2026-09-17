package ua.ztr.bmsble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BmsFrameParserTest {

    @Test
    fun `wrong length returns null`() {
        assertNull(BmsFrameParser.parse(ByteArray(10)))
        assertNull(BmsFrameParser.parse(ByteArray(20)))
    }

    @Test
    fun `unknown page type`() {
        val bytes = ByteArray(19)
        bytes[0] = 99
        val frame = BmsFrameParser.parse(bytes)
        assertTrue(frame is BmsFrame.Unknown)
        assertEquals(99, (frame as BmsFrame.Unknown).pageType)
    }

    @Test
    fun `page 1 basic info`() {
        val bytes = ByteArray(19)
        bytes[0] = 1
        putU16(bytes, 1, 5320)   // totalVoltage = 53.20 V
        putU16(bytes, 3, 125)    // current = 12.5 A
        putU16(bytes, 5, 1000)   // ratedCapacity = 100.0 Ah
        putU16(bytes, 7, 16)     // cellCount = 16
        putU16(bytes, 9, 100)    // unknownVoltage = 1.00 V
        putU16(bytes, 11, 250)   // temperature = 25.0 C

        putU16(bytes, 13, 532)   // secondaryVoltage = 53.2 V
        putU16(bytes, 15, 125)   // secondaryCurrent = 12.5 A
        putU16(bytes, 17, 28)    // powerKw = 2.8 kW

        val frame = BmsFrameParser.parse(bytes) as BmsFrame.BasicInfo
        assertEquals(53.20, frame.totalVoltage, 1e-9)
        assertEquals(12.5, frame.current, 1e-9)
        assertEquals(100.0, frame.ratedCapacityAh, 1e-9)
        assertEquals(16, frame.cellCount)
        assertEquals(1.00, frame.unknownVoltage, 1e-9)
        assertEquals(25.0, frame.temperatureC, 1e-9)
        assertEquals(53.2, frame.secondaryVoltage, 1e-9)
        assertEquals(12.5, frame.secondaryCurrent, 1e-9)
        assertEquals(2.8, frame.powerKw, 1e-9)
    }

    @Test
    fun `page 16 protection status and flags`() {
        val bytes = ByteArray(19)
        bytes[0] = 16
        putU16(bytes, 1, 5200)  // chargeRecoveryVoltage
        putU16(bytes, 3, 4400)  // dischargeRecoveryVoltage
        putU16(bytes, 5, 1)     // defaultChannelOn = true
        putU16(bytes, 7, 3)     // protectionCode = OVER_CHARGE
        putU16(bytes, 11, 1)    // screenOff = true
        bytes[18] = (0x40 or 0x20).toByte() // charging + balancing

        val frame = BmsFrameParser.parse(bytes) as BmsFrame.ProtectionStatus
        assertEquals(52.00, frame.chargeRecoveryVoltage, 1e-9)
        assertEquals(44.00, frame.dischargeRecoveryVoltage, 1e-9)
        assertTrue(frame.defaultChannelOn)
        assertEquals(ProtectionCode.OVER_CHARGE, frame.protectionCode)
        assertTrue(frame.screenOff)
        assertTrue(frame.status.isCharging)
        assertTrue(frame.status.isBalancing)
        assertTrue(!frame.status.alarmLowVoltage)
    }

    @Test
    fun `page 17 cell voltage diff is computed`() {
        val bytes = ByteArray(19)
        bytes[0] = 17
        putU16(bytes, 1, 250)   // lowVoltageHostShutdownVoltage = 2.50 V
        putU16(bytes, 5, 30)    // hostShutdownDelaySeconds = 30
        putU16(bytes, 9, 3250)  // minCellVoltage = 3.250 V
        putU16(bytes, 11, 3310) // maxCellVoltage = 3.310 V

        val frame = BmsFrameParser.parse(bytes) as BmsFrame.CellStats
        assertEquals(2.50, frame.lowVoltageHostShutdownVoltage, 1e-9)
        assertEquals(30, frame.hostShutdownDelaySeconds)
        assertEquals(3.250, frame.minCellVoltage, 1e-9)
        assertEquals(3.310, frame.maxCellVoltage, 1e-9)
        assertEquals(0.060, frame.cellVoltageDiff, 1e-6)
    }

    @Test
    fun `page 31 settings fields are fully identified`() {
        val bytes = ByteArray(19)
        bytes[0] = 31
        putU16(bytes, 1, 5)     // preChargeDelaySec
        putU16(bytes, 3, 50)    // cellVoltageDiffThreshold = 0.50 V
        putU16(bytes, 5, 1)     // autoResetCapacity = true
        putU16(bytes, 7, 20)    // lowTemperatureThreshold
        putU16(bytes, 9, 2)     // currentSensorType
        putU16(bytes, 11, 40)   // fanStartTemperatureC
        putU16(bytes, 13, 45)   // heaterStartTemperatureC
        putU16(bytes, 15, 100)  // canSendId
        putU16(bytes, 17, 200)  // canReceiveId

        val frame = (BmsFrameParser.parse(bytes) as BmsFrame.Settings).data
        assertEquals(5, frame.preChargeDelaySec)
        assertEquals(0.50, frame.cellVoltageDiffThreshold, 1e-9)
        assertTrue(frame.autoResetCapacity)
        assertEquals(20, frame.lowTemperatureThreshold)
        assertEquals(2, frame.currentSensorType)
        assertEquals(40, frame.fanStartTemperatureC)
        assertEquals(45, frame.heaterStartTemperatureC)
        assertEquals(100, frame.canSendId)
        assertEquals(200, frame.canReceiveId)
    }

    @Test
    fun `page 18 module temperatures with sign bits`() {
        val bytes = ByteArray(19)
        bytes[0] = 18
        bytes[2] = 0x81.toByte() // bit7 (probe1) and bit0 (probe8) negative
        putU16(bytes, 3, 250)    // probe1 magnitude = 25.0, negative -> -25.0
        putU16(bytes, 5, 100)    // probe2 = 10.0
        putU16(bytes, 7, 100)
        putU16(bytes, 9, 100)
        putU16(bytes, 11, 100)
        putU16(bytes, 13, 100)
        putU16(bytes, 15, 100)
        putU16(bytes, 17, 50)    // probe8 magnitude = 5.0, negative -> -5.0

        val frame = BmsFrameParser.parse(bytes) as BmsFrame.ModuleTemperatures
        assertEquals(8, frame.probesC.size)
        assertEquals(-25.0, frame.probesC[0], 1e-9)
        assertEquals(10.0, frame.probesC[1], 1e-9)
        assertEquals(-5.0, frame.probesC[7], 1e-9)
    }

    @Test
    fun `regular cell voltage page maps offsets to sequential cell numbers`() {
        val bytes = ByteArray(19)
        bytes[0] = 20 // cells 10-18
        putU16(bytes, 1, 3300)  // cell 10 = 3.300 V
        putU16(bytes, 17, 3350) // cell 18 = 3.350 V

        val frame = BmsFrameParser.parse(bytes) as BmsFrame.CellVoltages
        assertEquals(9, frame.cells.size)
        assertEquals(3.300, frame.cells[10]!!, 1e-9)
        assertEquals(3.350, frame.cells[18]!!, 1e-9)
    }

    @Test
    fun `page 24 carries both trigger cell and remainder cell voltages`() {
        val bytes = ByteArray(19)
        bytes[0] = 24
        putU16(bytes, 1, 3100)  // cell 46
        putU16(bytes, 3, 3200)  // cell 47
        putU16(bytes, 5, 3300)  // cell 48
        putU16(bytes, 7, 3400)  // cell 94
        putU16(bytes, 9, 3500)  // cell 95
        putU16(bytes, 11, 3600) // cell 96
        putU16(bytes, 17, 12)   // triggering cell number

        val frame = BmsFrameParser.parse(bytes) as BmsFrame.ProtectionTriggerCell
        assertEquals(12, frame.cellNumber)
        assertEquals(mapOf(46 to 3.100, 47 to 3.200, 48 to 3.300, 94 to 3.400, 95 to 3.500, 96 to 3.600), frame.remainderCells)
    }

    @Test
    fun `state reducer merges cell voltages across pages`() {
        var state = BmsState()
        val page20 = ByteArray(19).also { it[0] = 20; putU16(it, 1, 3300) }
        val page24 = ByteArray(19).also { it[0] = 24; putU16(it, 1, 3100) }

        state = BmsStateReducer.reduce(state, BmsFrameParser.parse(page20)!!, now = 1L)
        state = BmsStateReducer.reduce(state, BmsFrameParser.parse(page24)!!, now = 2L)

        assertEquals(3.300, state.cellVoltages[10]!!, 1e-9)
        assertEquals(3.100, state.cellVoltages[46]!!, 1e-9)
    }

    @Test
    fun `state reducer computes cumulative cycles from rated capacity`() {
        val basicInfoBytes = ByteArray(19)
        basicInfoBytes[0] = 1
        putU16(basicInfoBytes, 5, 1000) // ratedCapacity = 100.0 Ah
        val basicInfo = BmsFrameParser.parse(basicInfoBytes)!!

        val cellStatsBytes = ByteArray(19)
        cellStatsBytes[0] = 17
        putU32(cellStatsBytes, 15, 250) // cumulativeDischargeCapacityAh = 250.0
        val cellStats = BmsFrameParser.parse(cellStatsBytes)!!

        var state = BmsState()
        state = BmsStateReducer.reduce(state, basicInfo, now = 1L)
        state = BmsStateReducer.reduce(state, cellStats, now = 2L)

        assertEquals(2.5, state.cumulativeCycles!!, 1e-9)
    }

    @Test
    fun `simple command checksum is complement to 0xFFFF`() {
        assertArrayEqualsHex("0d0001fffe", BmsCommands.screenOn())
        assertArrayEqualsHex("0d0002fffd", BmsCommands.screenOff())
    }

    @Test
    fun `2-byte setting command checksum is complement to 0xFFFF`() {
        // param = 0x1388 (5000), checksum = 0xFFFF - 0x1388 = 0xEC77
        assertArrayEqualsHex("01" + "1388" + "ec77", BmsCommands.settingCommand(0x01, 5000))
        // discharge cutoff voltage 3.65 V -> param = 365 = 0x016D, checksum = 0xFE92
        assertArrayEqualsHex("01" + "016d" + "fe92", BmsCommands.dischargeCutoffVoltage(3.65))
        // total cell count 16 -> param = 0x0010, checksum = 0xFFEF
        assertArrayEqualsHex("04" + "0010" + "ffef", BmsCommands.totalCellCount(16))
    }

    @Test
    fun `channel and balance commands use confirmed 1-byte convention`() {
        assertArrayEqualsHex("070002fffd", BmsCommands.setChannel(open = true))
        assertArrayEqualsHex("070001fffe", BmsCommands.setChannel(open = false))
        assertArrayEqualsHex("080002fffd", BmsCommands.setAutoBalance(on = true))
        assertArrayEqualsHex("080001fffe", BmsCommands.setAutoBalance(on = false))
    }

    private fun putU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = ((value shr 8) and 0xFF).toByte()
        bytes[offset + 1] = (value and 0xFF).toByte()
    }

    private fun putU32(bytes: ByteArray, offset: Int, value: Long) {
        bytes[offset] = ((value shr 24) and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 16) and 0xFF).toByte()
        bytes[offset + 2] = ((value shr 8) and 0xFF).toByte()
        bytes[offset + 3] = (value and 0xFF).toByte()
    }

    private fun assertArrayEqualsHex(expectedHex: String, actual: ByteArray) {
        val actualHex = actual.joinToString("") { "%02x".format(it) }
        assertEquals(expectedHex, actualHex)
    }
}
