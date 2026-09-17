package ua.ztr.bmsble

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Пише сирі байти BLE-нотифікацій і події GATT-з'єднання у текстовий файл,
 * щоб діагностувати випадки на кшталт "підключення успішне, дані не з'являються"
 * без потреби знімати Bluetooth HCI snoop log.
 *
 * Файл створюється в app-specific зовнішньому сховищі
 * (`context.getExternalFilesDir(null)/logs/`) — доступ без storage-дозволів,
 * видаляється разом із застосунком. Останні [MAX_LINES] рядків додатково
 * тримаються в пам'яті ([lines]) для живого перегляду в UI.
 */
class BmsRawLogger(context: Context) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    /** Файл поточної сесії логування — новий файл створюється при кожному [BmsConnection]. */
    val file: File = run {
        val dir = File(context.getExternalFilesDir(null), "logs")
        dir.mkdirs()
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        // .txt, а не .log — деякі застосунки (напр. Viber) відхиляють поширення файлів
        // з нетиповим розширенням через share-інтент, хоча Android це не обмежує.
        File(dir, "bms_$name.txt")
    }

    private val _lines = MutableStateFlow<List<String>>(emptyList())

    /** Останні (до [MAX_LINES]) рядки логу — для живого відображення в UI. */
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    @Synchronized
    fun log(message: String) {
        val line = "${timeFormat.format(Date())} $message"
        _lines.update { (it + line).takeLast(MAX_LINES) }
        try {
            FileWriter(file, true).use { it.write("$line\n") }
        } catch (_: Exception) {
            // Діагностичне логування не повинно впливати на роботу з'єднання чи UI.
        }
    }

    fun logBytes(prefix: String, bytes: ByteArray) {
        log("$prefix size=${bytes.size} hex=${bytes.toHex()}")
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    private companion object {
        const val MAX_LINES = 500
    }
}
