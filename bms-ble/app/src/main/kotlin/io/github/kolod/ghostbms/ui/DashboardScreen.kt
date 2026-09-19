package io.github.kolod.ghostbms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import io.github.kolod.ghostbms.R
import io.github.kolod.ghostbms.ble.BmsConnectionState
import io.github.kolod.ghostbms.ble.BmsState
import io.github.kolod.ghostbms.ble.BmsStatusFlags
import io.github.kolod.ghostbms.ble.ProtectionCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * За замовчуванням перемикач очікує підтвердження від пристрою стільки, скільки триває
 * затримка передзаряду (пускове реле) + запас, перш ніж вважати живий стан з пристрою
 * достовірнішим за щойно натиснуте. Якщо налаштування ще невідоме — консервативний запас.
 */
private const val DEFAULT_OPTIMISTIC_LOCKOUT_MS = 3000L

@Composable
fun DashboardScreen(
    state: BmsState,
    logLines: List<String> = emptyList(),
    onScreenOn: () -> Unit,
    onScreenOff: () -> Unit,
    onBatteryEnabledChange: (Boolean) -> Unit,
    onAutoBalance: (Boolean) -> Unit,
    onShareLog: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Section(stringResource(R.string.section_live_readings)) { LiveReadingsCard(state) } }
            item { Section(stringResource(R.string.section_basic_settings_echo)) { ProtectionSettingsEchoCard(state) } }
            item { Section(stringResource(R.string.section_cells)) { CellStatsCard(state) } }
            item { Section(stringResource(R.string.section_status_protection)) { StatusCard(state) } }
            item { Section(stringResource(R.string.section_capacity_cycles)) { CapacityCard(state) } }
            state.settings?.let { settings ->
                item { Section(stringResource(R.string.section_settings)) { SettingsCard(settings) } }
            }
            item {
                Section(stringResource(R.string.section_control)) {
                    Column {
                        SwitchRow(
                            label = stringResource(R.string.switch_battery_connected),
                            checked = state.status?.channelOpen,
                            onCheckedChange = onBatteryEnabledChange,
                            // Вмикання йде через пускове реле з витримкою часу перед основним —
                            // живий стан "коло увімкнено" може ненадовго "просісти" під час
                            // цього перехідного процесу. Даємо йому весь час затримки + запас,
                            // перш ніж довіряти щойно прийнятому кадру більше за наш натиск.
                            optimisticLockoutMs = ((state.settings?.preChargeDelaySec ?: 3) * 1000L + 1500L)
                                .coerceIn(3000L, 15000L),
                        )
                        SwitchRow(
                            label = stringResource(R.string.switch_balancing),
                            checked = state.status?.isBalancing,
                            onCheckedChange = onAutoBalance,
                        )
                        SwitchRow(
                            label = stringResource(R.string.switch_screen),
                            checked = state.screenOff?.let { !it },
                            onCheckedChange = { on -> if (on) onScreenOn() else onScreenOff() },
                        )
                    }
                }
            }
            if (onShareLog != null) {
                item {
                    Section(stringResource(R.string.section_diagnostics)) {
                        DiagnosticsPanel(logLines = logLines, onShareLog = onShareLog)
                    }
                }
            }
            item { LastUpdatedRow(state.lastUpdated) }
        }
    }
}

/**
 * Верхня панель застосунку — завжди видима над навігацією між екранами (Дашборд/
 * Налаштування/Напруги комірок), з кнопкою-гамбургером для відкриття меню назви.
 */
@Composable
internal fun ConnectionBanner(
    deviceName: String?,
    state: BmsConnectionState,
    onMenuClick: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val (label, color) = when (state) {
        BmsConnectionState.DISCONNECTED -> stringResource(R.string.connection_state_disconnected) to ColorAlarm
        BmsConnectionState.CONNECTING -> stringResource(R.string.connection_state_connecting) to ColorWarning
        BmsConnectionState.DISCOVERING_SERVICES -> stringResource(R.string.connection_state_discovering) to ColorWarning
        BmsConnectionState.SUBSCRIBING -> stringResource(R.string.connection_state_subscribing) to ColorWarning
        BmsConnectionState.READY -> stringResource(R.string.connection_state_ready) to ColorOk
        BmsConnectionState.FAILED -> stringResource(R.string.connection_state_failed) to ColorAlarm
    }
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_content_description))
                }
                Column {
                    Text(deviceName ?: stringResource(R.string.default_device_name), style = MaterialTheme.typography.titleLarge)
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
                }
            }
            Button(onClick = onDisconnect) { Text(stringResource(R.string.disconnect_button)) }
        }
        HorizontalDivider()
    }
}

@Composable
private fun DiagnosticsPanel(logLines: List<String>, onShareLog: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) stringResource(R.string.log_hide)
                    else stringResource(R.string.log_show_realtime, logLines.size),
                )
            }
            OutlinedButton(onClick = onShareLog) { Text(stringResource(R.string.share_log_button)) }
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
 *
 * Показує щойно натиснуте значення ("оптимістично") протягом [optimisticLockoutMs] замість
 * живого [checked] з пристрою. Без цього перемикач, прив'язаний напряму до живого стану,
 * "відскакував" назад одразу після натискання, щойно приходив черговий кадр статусу до
 * завершення перехідного процесу на пристрої (наприклад, реле, увімкнене через пускове реле
 * з витримкою часу, — виглядало як "увімкнув і одразу вимкнув").
 */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean?,
    onCheckedChange: (Boolean) -> Unit,
    optimisticLockoutMs: Long = DEFAULT_OPTIMISTIC_LOCKOUT_MS,
) {
    var pending by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(checked) {
        if (pending != null && checked == pending) pending = null
    }
    LaunchedEffect(pending) {
        if (pending != null) {
            delay(optimisticLockoutMs)
            pending = null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = pending ?: checked ?: false,
            onCheckedChange = { new ->
                pending = new
                onCheckedChange(new)
            },
        )
    }
}

/** Сторінка 1, offset 13-18 — жива напруга/струм/потужність батареї. Підтверджено на пристрої. */
@Composable
private fun LiveReadingsCard(state: BmsState) {
    MetricRow(stringResource(R.string.metric_voltage), fmt(state.liveVoltage, stringResource(R.string.unit_v)))
    MetricRow(stringResource(R.string.metric_current), fmt(state.liveCurrent, stringResource(R.string.unit_a)))
    MetricRow(stringResource(R.string.metric_power), fmt(state.livePowerKw, stringResource(R.string.unit_kw)))
}

/**
 * Сторінка 1 — це НЕ жива телеметрія, а "відлуння" уставок захисту (підтверджено
 * власником пристрою). Живу напругу/струм батареї застосунок наразі не показує.
 */
@Composable
private fun ProtectionSettingsEchoCard(state: BmsState) {
    MetricRow(stringResource(R.string.metric_discharge_cutoff_voltage), fmt(state.dischargeCutoffVoltagePerCell, stringResource(R.string.unit_v_per_cell)))
    MetricRow(stringResource(R.string.metric_rated_relay_current), fmt(state.dischargeProtectionCurrent, stringResource(R.string.unit_a)))
    MetricRow(stringResource(R.string.metric_max_capacity), fmt(state.maxBatteryCapacityAh, stringResource(R.string.unit_ah)))
    MetricRow(stringResource(R.string.metric_cell_count), state.cellCount?.toString() ?: stringResource(R.string.value_missing))
    MetricRow(stringResource(R.string.metric_charge_cutoff_voltage), fmt(state.chargeCutoffVoltagePerCell, stringResource(R.string.unit_v_per_cell)))
    MetricRow(stringResource(R.string.metric_max_temp_threshold), fmt(state.highTemperatureProtectionThreshold, stringResource(R.string.unit_c)))
}

@Composable
private fun CellStatsCard(state: BmsState) {
    MetricRow(stringResource(R.string.metric_min_cell_voltage), fmt(state.minCellVoltage, stringResource(R.string.unit_v), 3))
    MetricRow(stringResource(R.string.metric_max_cell_voltage), fmt(state.maxCellVoltage, stringResource(R.string.unit_v), 3))
    MetricRow(stringResource(R.string.metric_voltage_diff), fmt(state.cellVoltageDiff, stringResource(R.string.unit_v), 3))
    MetricRow(stringResource(R.string.metric_balance_start_voltage), fmt(state.balanceStartVoltage, stringResource(R.string.unit_v)))
    MetricRow(stringResource(R.string.metric_balance_baseline_voltage), fmt(state.balanceBaselineVoltage, stringResource(R.string.unit_v), 3))
}

@Composable
private fun StatusCard(state: BmsState) {
    val status = state.status
    if (status != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            if (status.isCharging) StatusChip(stringResource(R.string.status_charging), ColorOk)
            if (status.isBalancing) StatusChip(stringResource(R.string.status_balancing), ColorOk)
            if (status.channelOpen) StatusChip(stringResource(R.string.status_channel_open), ColorOk)
        }
        val alarms = buildList {
            if (status.alarmLowVoltage) add(stringResource(R.string.alarm_low_voltage))
            if (status.alarmOverCurrent) add(stringResource(R.string.alarm_over_current))
            if (status.alarmWrongCellCount) add(stringResource(R.string.alarm_wrong_cell_count))
            if (status.alarmHighVoltage) add(stringResource(R.string.alarm_high_voltage))
            if (status.alarmHighTemperature) add(stringResource(R.string.alarm_high_temperature))
        }
        if (alarms.isNotEmpty()) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                alarms.forEach { StatusChip(it, ColorAlarm) }
            }
        }
    }
    MetricRow(stringResource(R.string.metric_protection_code), protectionLabel(state.protectionCode))
    if ((state.triggeringCellNumber ?: 0) > 0) {
        MetricRow(stringResource(R.string.label_triggering_cell), stringResource(R.string.label_cell_number, state.triggeringCellNumber!!))
    }
    val onLabel = stringResource(R.string.bool_on_short)
    val offLabel = stringResource(R.string.bool_off_short)
    MetricRow(stringResource(R.string.metric_charge_relay), boolLabel(state.chargeMosOn, onLabel, offLabel))
    MetricRow(stringResource(R.string.metric_discharge_relay), boolLabel(state.dischargeMosOn, onLabel, offLabel))
    MetricRow(stringResource(R.string.metric_relay_temperature), fmt(state.mosTemperatureC, stringResource(R.string.unit_c)))
    MetricRow(stringResource(R.string.metric_default_channel_on), boolLabel(state.defaultChannelOn, onLabel, offLabel))
    MetricRow(
        stringResource(R.string.metric_screen_state),
        boolLabel(state.screenOff, stringResource(R.string.screen_state_off), stringResource(R.string.screen_state_on)),
    )
    MetricRow(stringResource(R.string.metric_charge_recovery_voltage), fmt(state.chargeRecoveryVoltage, stringResource(R.string.unit_v)))
    MetricRow(stringResource(R.string.metric_discharge_recovery_voltage), fmt(state.dischargeRecoveryVoltage, stringResource(R.string.unit_v)))
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
    MetricRow(stringResource(R.string.metric_used_capacity), fmt(state.usedCapacityAh, stringResource(R.string.unit_ah)))
    MetricRow(stringResource(R.string.metric_cumulative_discharge), fmt(state.cumulativeDischargeCapacityAh, stringResource(R.string.unit_ah)))
    MetricRow(
        stringResource(R.string.metric_cumulative_cycles),
        state.cumulativeCycles?.let { "%.2f".format(it) } ?: stringResource(R.string.value_missing),
    )
}

@Composable
private fun SettingsCard(settings: io.github.kolod.ghostbms.ble.BmsSettings) {
    MetricRow(stringResource(R.string.label_precharge_delay), stringResource(R.string.value_seconds, settings.preChargeDelaySec))
    MetricRow(stringResource(R.string.label_cell_voltage_diff_threshold), stringResource(R.string.value_volts_2dp, settings.cellVoltageDiffThreshold))
    MetricRow(
        stringResource(R.string.label_auto_reset_capacity),
        stringResource(if (settings.autoResetCapacity) R.string.bool_on_short else R.string.bool_off_short),
    )
    MetricRow(stringResource(R.string.dashboard_low_temp_threshold), settings.lowTemperatureThreshold.toString())
    MetricRow(stringResource(R.string.label_current_sensor_type), settings.currentSensorType.toString())
}

@Composable
private fun protectionLabel(code: ProtectionCode?): String = when (code) {
    null -> stringResource(R.string.value_missing)
    ProtectionCode.NONE -> stringResource(R.string.protection_none)
    ProtectionCode.OVER_CURRENT -> stringResource(R.string.protection_over_current)
    ProtectionCode.OVER_DISCHARGE -> stringResource(R.string.protection_over_discharge)
    ProtectionCode.OVER_CHARGE -> stringResource(R.string.protection_over_charge)
    ProtectionCode.OVER_TEMPERATURE -> stringResource(R.string.protection_over_temperature)
    ProtectionCode.WRONG_CELL_COUNT -> stringResource(R.string.protection_wrong_cell_count)
    ProtectionCode.CHARGE_MOSFET_FAULT -> stringResource(R.string.protection_charge_mosfet_fault)
    ProtectionCode.DISCHARGE_MOSFET_FAULT -> stringResource(R.string.protection_discharge_mosfet_fault)
    ProtectionCode.LOW_VOLTAGE_SHUTDOWN -> stringResource(R.string.protection_low_voltage_shutdown)
    ProtectionCode.CELL_VOLTAGE_DIFF -> stringResource(R.string.protection_cell_voltage_diff)
    ProtectionCode.LOW_TEMPERATURE -> stringResource(R.string.protection_low_temperature)
    ProtectionCode.UNKNOWN -> stringResource(R.string.protection_unknown)
}

private fun boolLabel(value: Boolean?, whenTrue: String, whenFalse: String): String = when (value) {
    true -> whenTrue
    false -> whenFalse
    null -> "—"
}

@Composable
internal fun fmt(value: Double?, unit: String, decimals: Int = 2): String =
    if (value == null) stringResource(R.string.value_missing) else "%.${decimals}f %s".format(value, unit)

private fun formatTime(timestampMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))

/** Час останнього кадру, з якого оновлювався [BmsState] — спільний для всіх екранів. */
@Composable
internal fun LastUpdatedRow(lastUpdated: Long) {
    if (lastUpdated <= 0) return
    Text(
        stringResource(R.string.last_updated, formatTime(lastUpdated)),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}
