package ua.ztr.bmsble

/**
 * Побудова вихідних 5-байтних команд запису у характеристику FFE1.
 *
 * Формат: `[код_команди, 0x00, параметр, hi(0xFFFF-параметр), lo(0xFFFF-параметр)]`.
 * Контрольна сума — доповнення параметра до 0xFFFF (не CRC), підтверджено на
 * константах штатного застосунку — див. format.md.
 *
 * Підтверджено команду екрана (0x0D) і команду входу/виходу з екрана
 * реального часу (0x10) — штатний застосунок шле `10 00 02 FF FD` перед
 * відкриттям екрана показників (без цього пристрій не штовхає нотифікації
 * з даними) і `10 00 01 FF FE` при виході з нього / розриві з'єднання.
 * Інші коди команд (0x07-0x13), знайдені у декомпільованому коді, мають
 * невідоме призначення й тут не реалізовані. Команди зі складнішими
 * (2-байтними) параметрами — наприклад зміна порогових напруг захисту —
 * мають непідтверджену формулу контрольної суми; перед їх використанням
 * обов'язково звірте побайтово реальний трафік (Bluetooth HCI snoop log)
 * зі штатним застосунком.
 */
object BmsCommands {

    private const val CMD_SCREEN = 0x0D
    private const val PARAM_ON = 0x01
    private const val PARAM_OFF = 0x02

    private const val CMD_REALTIME = 0x10
    private const val PARAM_REALTIME_ENTER = 0x02
    private const val PARAM_REALTIME_EXIT = 0x01

    fun screenOn(): ByteArray = simpleCommand(CMD_SCREEN, PARAM_ON)
    fun screenOff(): ByteArray = simpleCommand(CMD_SCREEN, PARAM_OFF)

    /** Вмикає потік нотифікацій з показниками — шліть одразу після переходу з'єднання у READY. */
    fun enterRealtimeMonitoring(): ByteArray = simpleCommand(CMD_REALTIME, PARAM_REALTIME_ENTER)

    /** Штатний застосунок шле це перед виходом з екрана показників / розривом з'єднання. */
    fun exitRealtimeMonitoring(): ByteArray = simpleCommand(CMD_REALTIME, PARAM_REALTIME_EXIT)

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
