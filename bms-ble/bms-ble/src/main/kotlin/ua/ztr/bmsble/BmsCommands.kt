package ua.ztr.bmsble

/**
 * Побудова вихідних 5-байтних команд запису у характеристику FFE1.
 *
 * Формат: `[код_команди, 0x00, параметр, hi(0xFFFF-параметр), lo(0xFFFF-параметр)]`.
 * Контрольна сума — доповнення параметра до 0xFFFF (не CRC), підтверджено на
 * константах штатного застосунку — див. format.md.
 *
 * Підтверджено лише команду екрана (0x0D). Інші коди команд (0x07-0x13),
 * знайдені у декомпільованому коді, мають невідоме призначення й тут не
 * реалізовані. Команди зі складнішими (2-байтними) параметрами — наприклад
 * зміна порогових напруг захисту — мають непідтверджену формулу контрольної
 * суми; перед їх використанням обов'язково звірте побайтово реальний трафік
 * (Bluetooth HCI snoop log) зі штатним застосунком.
 */
object BmsCommands {

    private const val CMD_SCREEN = 0x0D
    private const val PARAM_ON = 0x01
    private const val PARAM_OFF = 0x02

    fun screenOn(): ByteArray = simpleCommand(CMD_SCREEN, PARAM_ON)
    fun screenOff(): ByteArray = simpleCommand(CMD_SCREEN, PARAM_OFF)

    /**
     * Загальна побудова 5-байтної команди з одним 1-байтним параметром.
     * [command] і [param] — значення 0..255.
     */
    fun simpleCommand(command: Int, param: Int): ByteArray {
        require(command in 0..0xFF) { "command має бути в діапазоні 0..255" }
        require(param in 0..0xFF) { "param має бути в діапазоні 0..255" }
        val checksum = 0xFFFF - param
        return byteArrayOf(
            command.toByte(),
            0x00,
            param.toByte(),
            ((checksum shr 8) and 0xFF).toByte(),
            (checksum and 0xFF).toByte(),
        )
    }
}
