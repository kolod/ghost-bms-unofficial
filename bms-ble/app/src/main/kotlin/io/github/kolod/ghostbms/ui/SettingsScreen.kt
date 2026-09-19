package io.github.kolod.ghostbms.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale
import io.github.kolod.ghostbms.R
import io.github.kolod.ghostbms.ble.BmsState

/**
 * Екран налаштувань BMS (аналог вікна1/窗口1 штатного застосунку). Формули запису для
 * майже всіх полів тут — ГІПОТЕЗА (checksum = 0xFFFF-параметр, екстрапольована з
 * підтвердженого 1-байтного випадку), не підтверджена HCI snoop-логом реального
 * трафіку. Кожне поле, що використовує цю гіпотезу, позначене ⚠ і попереджує в
 * діалозі редагування — звіряйте показник на пристрої після кожної зміни.
 */
@Composable
fun SettingsScreen(
    state: BmsState,
    onDischargeCutoffVoltage: (Double) -> Unit,
    onDischargeProtectionCurrent: (Double) -> Unit,
    onMaxBatteryCapacityAh: (Double) -> Unit,
    onTotalCellCount: (Int) -> Unit,
    onChargeCutoffVoltage: (Double) -> Unit,
    onHighTemperatureProtection: (Double) -> Unit,
    onChargeRecoveryVoltage: (Double) -> Unit,
    onDischargeRecoveryVoltage: (Double) -> Unit,
    onDefaultChannelState: (Boolean) -> Unit,
    onLowVoltageHostShutdown: (Double) -> Unit,
    onHostPowerOffDelaySec: (Int) -> Unit,
    onChargeBalanceVoltage: (Double) -> Unit,
    onUsedCapacityAh: (Int) -> Unit,
    onAutoResetCapacity: (Boolean) -> Unit,
    onPreChargeDelaySec: (Int) -> Unit,
    onCellVoltageDiffThreshold: (Double) -> Unit,
    onLowTemperatureThreshold: (Int) -> Unit,
    onCurrentSensorType: (Int) -> Unit,
    onFanStartTemperature: (Int) -> Unit,
    onHeaterStartTemperature: (Int) -> Unit,
    onCanSendId: (Int) -> Unit,
    onCanReceiveId: (Int) -> Unit,
    onClearAction9: () -> Unit,
    onResetDischargeCapacity: () -> Unit,
    onClearCycleCounter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorWarning.copy(alpha = 0.12f)),
                ) {
                    Text(
                        stringResource(R.string.settings_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            item {
                val unitV = stringResource(R.string.unit_v)
                Section(stringResource(R.string.section_voltages)) {
                    EditableSettingRow(
                        stringResource(R.string.metric_discharge_cutoff_voltage), unitV, state.dischargeCutoffVoltagePerCell, 2, 0.01, 5.00, false,
                    ) { onDischargeCutoffVoltage(it) }
                    EditableSettingRow(
                        stringResource(R.string.metric_charge_cutoff_voltage), unitV, state.chargeCutoffVoltagePerCell, 2, 0.01, 5.00, false,
                    ) { onChargeCutoffVoltage(it) }
                    EditableSettingRow(
                        stringResource(R.string.metric_charge_recovery_voltage), unitV, state.chargeRecoveryVoltage, 2, 0.01, 5.00, true,
                    ) { onChargeRecoveryVoltage(it) }
                    EditableSettingRow(
                        stringResource(R.string.metric_discharge_recovery_voltage), unitV, state.dischargeRecoveryVoltage, 2, 0.01, 5.00, true,
                    ) { onDischargeRecoveryVoltage(it) }
                    EditableSettingRow(
                        stringResource(R.string.label_low_voltage_host_shutdown), unitV, state.lowVoltageHostShutdownVoltage, 2, 0.01, 5.00, true,
                    ) { onLowVoltageHostShutdown(it) }
                    EditableSettingRow(
                        stringResource(R.string.metric_balance_start_voltage), unitV, state.balanceStartVoltage, 2, 0.01, 5.00, true,
                    ) { onChargeBalanceVoltage(it) }
                    EditableSettingRow(
                        stringResource(R.string.label_cell_voltage_diff_threshold), unitV, settings?.cellVoltageDiffThreshold, 2, 0.0, 5.00, true,
                    ) { onCellVoltageDiffThreshold(it) }
                }
            }

            item {
                val unitC = stringResource(R.string.unit_c)
                Section(stringResource(R.string.section_current_temperature)) {
                    EditableSettingRow(
                        stringResource(R.string.metric_rated_relay_current), stringResource(R.string.unit_a), state.dischargeProtectionCurrent, 1, 0.0, 999.9, false,
                    ) { onDischargeProtectionCurrent(it) }
                    EditableSettingRow(
                        stringResource(R.string.settings_overtemp_protection), unitC, state.highTemperatureProtectionThreshold, 1, 0.0, 150.0, false,
                    ) { onHighTemperatureProtection(it) }
                    EditableSettingRow(
                        stringResource(R.string.settings_low_temp_protection), unitC, settings?.lowTemperatureThreshold?.toDouble(),
                        0, 0.0, 100.0, true,
                    ) { onLowTemperatureThreshold(it.toInt()) }
                    EditableSettingRow(
                        stringResource(R.string.label_fan_start_temp), unitC, settings?.fanStartTemperatureC?.toDouble(),
                        0, 0.0, 100.0, true,
                    ) { onFanStartTemperature(it.toInt()) }
                    EditableSettingRow(
                        stringResource(R.string.label_heater_start_temp), unitC, settings?.heaterStartTemperatureC?.toDouble(),
                        0, 0.0, 100.0, true,
                    ) { onHeaterStartTemperature(it.toInt()) }
                }
            }

            item {
                val onLabel = stringResource(R.string.bool_on_short)
                val offLabel = stringResource(R.string.bool_off_short)
                Section(stringResource(R.string.section_capacity_cells)) {
                    EditableSettingRow(
                        stringResource(R.string.metric_max_capacity), stringResource(R.string.unit_ah), state.maxBatteryCapacityAh, 1, 0.0, 6500.0, false,
                    ) { onMaxBatteryCapacityAh(it) }
                    EditableSettingRow(
                        stringResource(R.string.metric_cell_count), stringResource(R.string.unit_pcs), state.cellCount?.toDouble(), 0, 0.0, 192.0, true,
                    ) { onTotalCellCount(it.toInt()) }
                    EditableSettingRow(
                        stringResource(R.string.metric_used_capacity), stringResource(R.string.unit_ah), state.usedCapacityAh, 0, 0.0, 6500.0, true,
                    ) { onUsedCapacityAh(it.toInt()) }
                    BooleanSettingRow(
                        stringResource(R.string.label_auto_reset_capacity), settings?.autoResetCapacity, onLabel, offLabel,
                        onOn = { onAutoResetCapacity(true) },
                        onOff = { onAutoResetCapacity(false) },
                    )
                }
            }

            item {
                Section(stringResource(R.string.section_delays)) {
                    EditableSettingRow(
                        stringResource(R.string.label_host_shutdown_delay), stringResource(R.string.unit_s), state.hostShutdownDelaySeconds?.toDouble(),
                        0, 1.0, 60000.0, true,
                    ) { onHostPowerOffDelaySec(it.toInt()) }
                    EditableSettingRow(
                        stringResource(R.string.label_precharge_delay), stringResource(R.string.unit_s), settings?.preChargeDelaySec?.toDouble(),
                        0, 0.0, 6500.0, true,
                    ) { onPreChargeDelaySec(it.toInt()) }
                }
            }

            item {
                Section(stringResource(R.string.section_default_channel)) {
                    BooleanSettingRow(
                        stringResource(R.string.section_default_channel), state.defaultChannelOn,
                        stringResource(R.string.bool_on_short), stringResource(R.string.bool_off_short),
                        onOn = { onDefaultChannelState(true) },
                        onOff = { onDefaultChannelState(false) },
                    )
                }
            }

            item {
                Section(stringResource(R.string.section_engineering)) {
                    EditableSettingRow(
                        stringResource(R.string.label_current_sensor_type_engineering), "", settings?.currentSensorType?.toDouble(), 0, 1.0, 3.0, true,
                    ) { onCurrentSensorType(it.toInt()) }
                    EditableSettingRow(
                        stringResource(R.string.label_can_send_id), "", settings?.canSendId?.toDouble(), 0, 1.0, 32000.0, true,
                    ) { onCanSendId(it.toInt()) }
                    EditableSettingRow(
                        stringResource(R.string.label_can_receive_id), "", settings?.canReceiveId?.toDouble(), 0, 1.0, 32000.0, true,
                    ) { onCanReceiveId(it.toInt()) }
                }
            }

            item {
                Section(stringResource(R.string.section_reset)) {
                    Text(
                        stringResource(R.string.reset_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorWarning,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClearAction9) { Text(stringResource(R.string.reset_action9_button)) }
                        OutlinedButton(onClick = onResetDischargeCapacity) { Text(stringResource(R.string.reset_discharge_capacity_button)) }
                        OutlinedButton(onClick = onClearCycleCounter) { Text(stringResource(R.string.reset_cycle_counter_button)) }
                    }
                }
            }

            item { LastUpdatedRow(state.lastUpdated) }
        }
    }
}

@Composable
private fun BooleanSettingRow(
    label: String,
    currentValue: Boolean?,
    onLabel: String,
    offLabel: String,
    onOn: () -> Unit,
    onOff: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                when (currentValue) { true -> onLabel; false -> offLabel; null -> stringResource(R.string.value_missing) },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(onClick = onOn) { Text(onLabel) }
            OutlinedButton(onClick = onOff) { Text(offLabel) }
        }
    }
}

@Composable
private fun EditableSettingRow(
    label: String,
    unit: String,
    currentValue: Double?,
    decimals: Int,
    min: Double,
    max: Double,
    unconfirmed: Boolean,
    onSubmit: (Double) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showDialog = true },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            currentValue?.let { "%.${decimals}f %s".format(it, unit).trim() } ?: stringResource(R.string.value_missing_tap_to_set),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (showDialog) {
        SettingEditDialog(
            label = label,
            unit = unit,
            initialValue = currentValue,
            decimals = decimals,
            min = min,
            max = max,
            unconfirmed = unconfirmed,
            onDismiss = { showDialog = false },
            onSubmit = {
                onSubmit(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun SettingEditDialog(
    label: String,
    unit: String,
    initialValue: Double?,
    decimals: Int,
    min: Double,
    max: Double,
    unconfirmed: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Double) -> Unit,
) {
    // Locale.ROOT — щоб десятковим роздільником завжди була крапка (лишень такий
    // формат приймає toDoubleOrNull() нижче; на пристроях з укр. локаллю
    // String.format без Locale підставляв кому, і поле одразу ставало невалідним).
    var text by remember {
        mutableStateOf(initialValue?.let { String.format(Locale.ROOT, "%.${decimals}f", it) } ?: "")
    }
    // Кому теж приймаємо — деякі клавіатури вставляють її як десятковий роздільник
    // навіть у режимі Decimal, залежно від мовної розкладки.
    val parsed = text.replace(',', '.').toDoubleOrNull()
    val valid = parsed != null && parsed in min..max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column {
                if (unconfirmed) {
                    Text(
                        stringResource(R.string.dialog_unconfirmed_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorWarning,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(unit.ifBlank { stringResource(R.string.field_generic_value_label) }) },
                    isError = !valid && text.isNotEmpty(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (decimals == 0) KeyboardType.Number else KeyboardType.Decimal,
                    ),
                )
                Text(
                    stringResource(R.string.dialog_limits, min.toString(), max.toString(), unit).trim(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = { parsed?.let(onSubmit) }, enabled = valid) { Text(stringResource(R.string.dialog_submit_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel_button)) }
        },
    )
}
