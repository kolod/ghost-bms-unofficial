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

        val frame = BmsFrameParser.parse(bytes) as BmsFrame.BasicInfo
        assertEquals(53.20, frame.totalVoltage, 1e-9)
        assertEquals(12.5, frame.current, 1e-9)
        assertEquals(100.0, frame.ratedCapacityAh, 1e-9)
        assertEquals(16, frame.cellCount)
        assertEquals(1.00, frame.unknownVoltage, 1e-9)
        assertEquals(25.0, frame.temperatureC, 1e-9)
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
        putU16(bytes, 9, 3250)  // minCellVoltage = 3.250 V
        putU16(bytes, 11, 3310) // maxCellVoltage = 3.310 V

        val frame = BmsFrameParser.parse(bytes) as BmsFrame.CellStats
        assertEquals(3.250, frame.minCellVoltage, 1e-9)
        assertEquals(3.310, frame.maxCellVoltage, 1e-9)
        assertEquals(0.060, frame.cellVoltageDiff, 1e-6)
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
