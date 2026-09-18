package ua.ztr.bmsmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.ztr.bmsble.BmsConnectionState
import ua.ztr.bmsble.BmsState
import ua.ztr.bmsble.BmsStatusFlags
import ua.ztr.bmsble.ProtectionCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    deviceName: String?,
    connectionState: BmsConnectionState,
    state: BmsState,
    logLines: List<String> = emptyList(),
    onScreenOn: () -> Unit,
    onScreenOff: () -> Unit,
    onBatteryEnabledChange: (Boolean) -> Unit,
    onAutoBalance: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onShareLog: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ConnectionBanner(deviceName, connectionState, onDisconnect)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Section("Живі показники") { LiveReadingsCard(state) } }
            item { Section("Основні налаштування") { ProtectionSettingsEchoCard(state) } }
            item { Section("Комірки") { CellStatsCard(state) } }
            item { Section("Статус і захист") { StatusCard(state) } }
            item { Section("Ємність і цикли") { CapacityCard(state) } }
            state.settings?.let { settings ->
                item { Section("Налаштування") { SettingsCard(settings) } }
            }
            item {
                Section("Керування") {
                    Column {
                        SwitchRow(
                            label = "Батарея підключена",
                            checked = state.status?.channelOpen,
                            onCheckedChange = onBatteryEnabledChange,
                        )
                        SwitchRow(
                            label = "Балансування",
                            checked = state.status?.isBalancing,
                            onCheckedChange = onAutoBalance,
                        )
                        SwitchRow(
                            label = "Екран",
                            checked = state.screenOff?.let { !it },
                            onCheckedChange = { on -> if (on) onScreenOn() else onScreenOff() },
                        )
                    }
                }
            }
            if (onShareLog != null) {
                item {
                    Section("Діагностика") {
                        DiagnosticsPanel(logLines = logLines, onShareLog = onShareLog)
                    }
                }
            }
            state.lastUpdated.takeIf { it > 0 }?.let { ts ->
                item {
                    Text(
                        "Останнє оновлення: ${formatTime(ts)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionBanner(deviceName: String?, state: BmsConnectionState, onDisconnect: () -> Unit) {
    val (label, color) = when (state) {
        BmsConnectionState.DISCONNECTED -> "Відключено" to ColorAlarm
        BmsConnectionState.CONNECTING -> "Підключення…" to ColorWarning
        BmsConnectionState.DISCOVERING_SERVICES -> "Пошук сервісів…" to ColorWarning
        BmsConnectionState.SUBSCRIBING -> "Підписка на дані…" to ColorWarning
        BmsConnectionState.READY -> "Підключено" to ColorOk
        BmsConnectionState.FAILED -> "Помилка з'єднання" to ColorAlarm
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(deviceName ?: "BMS", style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
        }
        Button(onClick = onDisconnect) { Text("Відключити") }
    }
    HorizontalDivider()
}

@Composable
private fun DiagnosticsPanel(logLines: List<String>, onShareLog: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Сховати лог" else "Лог у реальному часі (${logLines.size})")
            }
            OutlinedButton(onClick = onShareLog) { Text("Поділитися логом") }
        }
        if (expanded) {
            val listState = rememberLazyListState()
            LaunchedEffect(logLines.size) {
                if (logLines.isNotEmpty()) listState.animateScrollToItem(logLines.size - 1)
            }
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .padding(top = 8.dp)
                        .background(Color.Black),
                ) {
                    items(logLines) { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF33FF33),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            colors = CardDefaults.cardColors(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) { content() }
        }
    }
}

@Composable
internal fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Рядок керування перемикачем. [checked] — живий стан з пристрою (`null`, доки не прийшов
 * перший кадр — тоді перемикач показує "вимкнено" за замовчуванням, але це не підтверджений факт).
 */
@Composable
private fun SwitchRow(label: String, checked: Boolean?, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked ?: false, onCheckedChange = onCheckedChange)
    }
}

/** Сторінка 1, offset 13-18 — жива напруга/струм/потужність батареї. Підтверджено на пристрої. */
@Composable
private fun LiveReadingsCard(state: BmsState) {
    MetricRow("Напруга", fmt(state.liveVoltage, "В"))
    MetricRow("Струм", fmt(state.liveCurrent, "А"))
    MetricRow("Потужність", fmt(state.livePowerKw, "кВт"))
}

/**
 * Сторінка 1 — це НЕ жива телеметрія, а "відлуння" уставок захисту (підтверджено
 * власником пристрою). Живу напругу/струм батареї застосунок наразі не показує.
 */
@Composable
private fun ProtectionSettingsEchoCard(state: BmsState) {
    MetricRow("Напруга відсічки розряду", fmt(state.dischargeCutoffVoltagePerCell, "В/комірку"))
    MetricRow("Номінальний струм реле", fmt(state.dischargeProtectionCurrent, "А"))
    MetricRow("Максимальна ємність", fmt(state.maxBatteryCapacityAh, "Аг"))
    MetricRow("Кількість комірок", state.cellCount?.toString() ?: "—")
    MetricRow("Напруга відсічки заряду", fmt(state.chargeCutoffVoltagePerCell, "В/комірку"))
    MetricRow("Уставка макс. температури", fmt(state.highTemperatureProtectionThreshold, "°C"))
}

@Composable
private fun CellStatsCard(state: BmsState) {
    MetricRow("Мін. напруга комірки", fmt(state.minCellVoltage, "В", 3))
    MetricRow("Макс. напруга комірки", fmt(state.maxCellVoltage, "В", 3))
    MetricRow("Різниця (макс-мін)", fmt(state.cellVoltageDiff, "В", 3))
    MetricRow("Напруга старту балансування", fmt(state.balanceStartVoltage, "В"))
    MetricRow("Опорна напруга балансування", fmt(state.balanceBaselineVoltage, "В", 3))
}

@Composable
private fun StatusCard(state: BmsState) {
    val status = state.status
    if (status != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            if (status.isCharging) StatusChip("Заряджається", ColorOk)
            if (status.isBalancing) StatusChip("Балансування", ColorOk)
            if (status.channelOpen) StatusChip("Коло увімкнено", ColorOk)
        }
        val alarms = buildList {
            if (status.alarmLowVoltage) add("Напруга нижче порогу")
            if (status.alarmOverCurrent) add("Струм вище порогу")
            if (status.alarmWrongCellCount) add("Невірна кількість комірок")
            if (status.alarmHighVoltage) add("Напруга вище порогу")
            if (status.alarmHighTemperature) add("Температура вище порогу")
        }
        if (alarms.isNotEmpty()) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                alarms.forEach { StatusChip(it, ColorAlarm) }
            }
        }
    }
    MetricRow("Код захисту", protectionLabel(state.protectionCode))
    if ((state.triggeringCellNumber ?: 0) > 0) {
        MetricRow("Комірка, що спричинила захист", "№${state.triggeringCellNumber}")
    }
    MetricRow("Реле заряду", boolLabel(state.chargeMosOn, "увімк.", "вимк."))
    MetricRow("Реле розряду", boolLabel(state.dischargeMosOn, "увімк.", "вимк."))
    MetricRow("Температура реле (MOSFET)", fmt(state.mosTemperatureC, "°C"))
    MetricRow("Батарея увімкнена за замовчуванням", boolLabel(state.defaultChannelOn, "увімк.", "вимк."))
    MetricRow("Екран", boolLabel(state.screenOff, "вимкнено", "увімкнено"))
    MetricRow("Напруга відновлення заряду", fmt(state.chargeRecoveryVoltage, "В"))
    MetricRow("Напруга відновлення розряду", fmt(state.dischargeRecoveryVoltage, "В"))
}

@Composable
private fun StatusChip(text: String, color: Color) {
    AssistChip(
        onClick = {},
        label = { Text(text) },
        modifier = Modifier.wrapContentWidth(),
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
    )
}

@Composable
private fun CapacityCard(state: BmsState) {
    MetricRow("Використана ємність", fmt(state.usedCapacityAh, "Аг"))
    MetricRow("Кумулятивна розряджена ємність", fmt(state.cumulativeDischargeCapacityAh, "Аг"))
    MetricRow("Кумулятивні цикли (розрах.)", state.cumulativeCycles?.let { "%.2f".format(it) } ?: "—")
}

@Composable
private fun SettingsCard(settings: ua.ztr.bmsble.BmsSettings) {
    MetricRow("Затримка передзаряду", "${settings.preChargeDelaySec} с")
    MetricRow("Поріг різниці напруг комірок", "%.2f В".format(settings.cellVoltageDiffThreshold))
    MetricRow("Автоскидання ємності", if (settings.autoResetCapacity) "увімк." else "вимк.")
    MetricRow("Поріг низької температури", settings.lowTemperatureThreshold.toString())
    MetricRow("Тип датчика струму", settings.currentSensorType.toString())
}

private fun protectionLabel(code: ProtectionCode?): String =
    if (code == null) "—" else code.description

private fun boolLabel(value: Boolean?, whenTrue: String, whenFalse: String): String = when (value) {
    true -> whenTrue
    false -> whenFalse
    null -> "—"
}

internal fun fmt(value: Double?, unit: String, decimals: Int = 2): String =
    if (value == null) "—" else "%.${decimals}f %s".format(value, unit)

private fun formatTime(timestampMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
