package io.github.kolod.ghostbms.ble

import kotlin.math.roundToInt

/**
 * Побудова вихідних 5-байтних команд запису у характеристику FFE1.
 *
 * Формат: `[код_команди, hi(параметр), lo(параметр), hi(0xFFFF-параметр), lo(0xFFFF-параметр)]`.
 * Контрольна сума — доповнення параметра до 0xFFFF (не CRC).
 *
 * Підтверджено на реальному пристрої (1-байтний параметр, `hi(параметр)` завжди 0x00):
 * команда екрана (0x0D), вхід/вихід з екрана реального часу (0x10, обов'язковий —
 * без нього BMS не починає штовхати нотифікації з даними), увімкнення/вимкнення
 * батареї (0x07) і автобалансування (0x08).
 *
 * Команди з 2-байтним параметром спочатку були **гіпотезою** (той самий принцип
 * checksum, екстрапольований з 1-байтного випадку, без HCI snoop-підтвердження).
 * Відтоді на реальному пристрої підтверджено запис: 0x01 (напруга відсічки
 * розряду), 0x02 (номінальний струм реле), 0x03 (максимальна ємність), 0x05
 * (напруга відсічки заряду), 0x06 (захист від перегріву) — формула checksum
 * для 2-байтних параметрів працює. Решта команд із 2-байтним параметром
 * лишаються неперевіреними гіпотезами — перед довірою до них звіряйте показник
 * на пристрої після кожної зміни (в UI позначені попередженням).
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
     * Увімкнути/вимкнути батарею (силове коло) — НЕ окремо заряд чи розряд: якщо напруга
     * батареї в допустимому діапазоні, BMS сама вмикає потрібні реле (заряду і/або розряду).
     * Підтверджено (1-байтний параметр). Раніше називалась "канал" — назва зі штатного
     * застосунку ("通道", channel) вводила в оману щодо реального призначення.
     */
    fun setBatteryEnabled(enabled: Boolean): ByteArray = simpleCommand(0x07, if (enabled) 2 else 1)

    /** Автоматичне балансування комірок. Підтверджено (1-байтний параметр). */
    fun setAutoBalance(on: Boolean): ByteArray = simpleCommand(0x08, if (on) 2 else 1)

    /**
     * Кнопка "Скинути" біля накопиченої розрядженої ємності (C1.java:5673). Точна ціль
     * не підтверджена — можливо перетинається з [resetDischargeCapacity]/[clearCycleCounter].
     */
    fun clearAction9(): ByteArray = simpleCommand(0x09, 1)

    /** Нижня кнопка скидання накопиченої ємності (C1.java:5697). Ціль не підтверджена остаточно. */
    fun resetDischargeCapacity(): ByteArray = simpleCommand(0x12, 1)

    /** Кнопка "CLR" біля кумулятивних циклів (C1.java:5721). */
    fun clearCycleCounter(): ByteArray = simpleCommand(0x13, 1)

    // --- 2-байтні параметри ---

    /** ПІДТВЕРДЖЕНО на пристрої. 0.01–5.00 В/комірку. Readout: [BmsState.dischargeCutoffVoltagePerCell] (сторінка 1). */
    fun dischargeCutoffVoltage(volts: Double): ByteArray = settingCommand(0x01, (volts * 100).roundToInt())

    /** ПІДТВЕРДЖЕНО на пристрої. 0–999.9 А, номінальний струм реле. Readout: [BmsState.dischargeProtectionCurrent] (сторінка 1). */
    fun dischargeProtectionCurrent(amps: Double): ByteArray = settingCommand(0x02, (amps * 10).roundToInt())

    /** ПІДТВЕРДЖЕНО на пристрої. 0–6500.0 Аг. Readout: [BmsState.maxBatteryCapacityAh] (сторінка 1). */
    fun maxBatteryCapacityAh(ah: Double): ByteArray = settingCommand(0x03, (ah * 10).roundToInt())

    /** Гіпотеза (не перевірено). 0–192 комірок. Readout: [BmsState.cellCount] (сторінка 1). */
    fun totalCellCount(count: Int): ByteArray = settingCommand(0x04, count)

    /** ПІДТВЕРДЖЕНО на пристрої. 0.01–5.00 В/комірку. Readout: [BmsState.chargeCutoffVoltagePerCell] (сторінка 1). */
    fun chargeCutoffVoltage(volts: Double): ByteArray = settingCommand(0x05, (volts * 100).roundToInt())

    /** ПІДТВЕРДЖЕНО на пристрої. 0–150.0 °C. Readout: [BmsState.highTemperatureProtectionThreshold] (сторінка 1). */
    fun highTemperatureProtection(celsius: Double): ByteArray = settingCommand(0x06, (celsius * 10).roundToInt())

    /** 0.01–5.00 В. Readout: [BmsState.chargeRecoveryVoltage] (сторінка 16). */
    fun chargeRecoveryVoltage(volts: Double): ByteArray = settingCommand(0x0A, (volts * 100).roundToInt())

    /** 0.01–5.00 В. Readout: [BmsState.dischargeRecoveryVoltage] (сторінка 16). */
    fun dischargeRecoveryVoltage(volts: Double): ByteArray = settingCommand(0x0B, (volts * 100).roundToInt())

    /**
     * Стан каналу за замовчуванням. Увага: конвенція on/off тут ІНША, ніж у [setBatteryEnabled]/
     * [setAutoBalance] (тут 1=on/2=off, там 2=on/1=off) — не уніфікувати помилково.
     * Readout: [BmsState.defaultChannelOn] (сторінка 16).
     */
    fun defaultChannelState(on: Boolean): ByteArray = settingCommand(0x0C, if (on) 1 else 2)

    /** 0.01–5.00 В. Readout: `CellStats.lowVoltageHostShutdownVoltage` (сторінка 17). */
    fun lowVoltageHostShutdown(volts: Double): ByteArray = settingCommand(0x0E, (volts * 100).roundToInt())

    /** 1–60000 с. Readout: `CellStats.hostShutdownDelaySeconds` (сторінка 17). */
    fun hostPowerOffDelaySec(seconds: Int): ByteArray = settingCommand(0x0F, seconds)

    /** 0.01–5.00 В. Readout: [BmsState.balanceStartVoltage] (сторінка 17). */
    fun chargeBalanceVoltage(volts: Double): ByteArray = settingCommand(0x11, (volts * 100).roundToInt())

    /** 0–6500 (без масштабування — на відміну від [maxBatteryCapacityAh]!). Readout: [BmsState.usedCapacityAh]. */
    fun usedCapacityAh(ah: Int): ByteArray = settingCommand(0x14, ah)

    /**
     * Увага: конвенція on/off тут ІНША, ніж у [setBatteryEnabled]/[setAutoBalance] (тут 1=on/2=off).
     * Readout: `BmsSettings.autoResetCapacity` (сторінка 31).
     */
    fun autoResetCapacity(on: Boolean): ByteArray = settingCommand(0x15, if (on) 1 else 2)

    /** 0–6500 с. Readout: `BmsSettings.preChargeDelaySec` (сторінка 31). */
    fun preChargeDelaySec(seconds: Int): ByteArray = settingCommand(0x16, seconds)

    /** 0–5.00 В. Readout: `BmsSettings.cellVoltageDiffThreshold` (сторінка 31). */
    fun cellVoltageDiffThreshold(volts: Double): ByteArray = settingCommand(0x17, (volts * 100).roundToInt())

    /** 0–100 °C (від'ємні). Readout: `BmsSettings.lowTemperatureThreshold` (сторінка 31). */
    fun lowTemperatureThreshold(celsius: Int): ByteArray = settingCommand(0x18, celsius)

    /**
     * 1–3 (enum типу датчика струму). Захищено паролем "770921" у штатному застосунку.
     * Readout: `BmsSettings.currentSensorType` (сторінка 31).
     */
    fun currentSensorType(type: Int): ByteArray = settingCommand(0x19, type)

    /** 0–100 °C. Readout: `BmsSettings.fanStartTemperatureC` (сторінка 31). */
    fun fanStartTemperature(celsius: Int): ByteArray = settingCommand(0x1A, celsius)

    /** 0–100 °C. Readout: `BmsSettings.heaterStartTemperatureC` (сторінка 31). */
    fun heaterStartTemperature(celsius: Int): ByteArray = settingCommand(0x1B, celsius)

    /**
     * 1–32000, має відрізнятись від [canReceiveId]. Захищено паролем "770921" у штатному
     * застосунку. Readout: `BmsSettings.canSendId` (сторінка 31).
     */
    fun canSendId(id: Int): ByteArray = settingCommand(0x1C, id)

    /**
     * 1–32000, має відрізнятись від [canSendId]. Захищено паролем "770921" у штатному
     * застосунку. Readout: `BmsSettings.canReceiveId` (сторінка 31).
     */
    fun canReceiveId(id: Int): ByteArray = settingCommand(0x1D, id)

    /**
     * Загальна побудова 5-байтної команди з одним 1-байтним параметром.
     * [command] і [param] — значення 0..255.
     */
    fun simpleCommand(command: Int, param: Int): ByteArray {
        require(command in 0..0xFF) { "command має бути в діапазоні 0..255" }
        require(param in 0..0xFF) { "param має бути в діапазоні 0..255" }
        return settingCommand(command, param)
    }

    /**
     * 5-байтна команда з 2-байтним параметром (0..0xFFFF). Формула контрольної суми —
     * ГІПОТЕЗА: той самий принцип 0xFFFF-param, що підтверджений для 1-байтних команд,
     * екстрапольований на 2-байтний випадок. НЕ підтверджено HCI snoop-логом реального
     * трафіку — перед довірою до запису порогових напруг/температур звіряйте показник
     * на пристрої після кожної зміни.
     */
    fun settingCommand(command: Int, param: Int): ByteArray {
        require(command in 0..0xFF) { "command має бути в діапазоні 0..255" }
        require(param in 0..0xFFFF) { "param має бути в діапазоні 0..0xFFFF" }
        val checksum = 0xFFFF - param
        return byteArrayOf(
            command.toByte(),
            ((param shr 8) and 0xFF).toByte(),
            (param and 0xFF).toByte(),
            ((checksum shr 8) and 0xFF).toByte(),
            (checksum and 0xFF).toByte(),
        )
    }
}
