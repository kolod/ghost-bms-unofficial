package ua.ztr.bmsmonitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import ua.ztr.bmsble.BmsState

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
                        "⚠ Формули запису для більшості полів нижче не перевірені на реальному пристрої " +
                            "(гіпотеза, не HCI-підтверджена). Після кожної зміни звіряйте показник у штатному " +
                            "застосунку, перш ніж довіряти наступному полю.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            item {
                Section("Напруги") {
                    EditableSettingRow(
                        "Напруга відсічки розряду", "В", state.dischargeCutoffVoltagePerCell, 2, 0.01, 5.00, false,
                    ) { onDischargeCutoffVoltage(it) }
                    EditableSettingRow(
                        "Напруга відсічки заряду", "В", state.chargeCutoffVoltagePerCell, 2, 0.01, 5.00, false,
                    ) { onChargeCutoffVoltage(it) }
                    EditableSettingRow(
                        "Напруга відновлення заряду", "В", state.chargeRecoveryVoltage, 2, 0.01, 5.00, true,
                    ) { onChargeRecoveryVoltage(it) }
                    EditableSettingRow(
                        "Напруга відновлення розряду", "В", state.dischargeRecoveryVoltage, 2, 0.01, 5.00, true,
                    ) { onDischargeRecoveryVoltage(it) }
                    EditableSettingRow(
                        "Низька напруга відключення хоста", "В", state.lowVoltageHostShutdownVoltage, 2, 0.01, 5.00, true,
                    ) { onLowVoltageHostShutdown(it) }
                    EditableSettingRow(
                        "Напруга старту балансування", "В", state.balanceStartVoltage, 2, 0.01, 5.00, true,
                    ) { onChargeBalanceVoltage(it) }
                    EditableSettingRow(
                        "Поріг різниці напруг комірок", "В", settings?.cellVoltageDiffThreshold, 2, 0.0, 5.00, true,
                    ) { onCellVoltageDiffThreshold(it) }
                }
            }

            item {
                Section("Струм і температура") {
                    EditableSettingRow(
                        "Номінальний струм реле", "А", state.dischargeProtectionCurrent, 1, 0.0, 999.9, false,
                    ) { onDischargeProtectionCurrent(it) }
                    EditableSettingRow(
                        "Захист від перегріву", "°C", state.highTemperatureProtectionThreshold, 1, 0.0, 150.0, false,
                    ) { onHighTemperatureProtection(it) }
                    EditableSettingRow(
                        "Захист від низької температури", "°C", settings?.lowTemperatureThreshold?.toDouble(),
                        0, 0.0, 100.0, true,
                    ) { onLowTemperatureThreshold(it.toInt()) }
                    EditableSettingRow(
                        "Температура старту вентилятора", "°C", settings?.fanStartTemperatureC?.toDouble(),
                        0, 0.0, 100.0, true,
                    ) { onFanStartTemperature(it.toInt()) }
                    EditableSettingRow(
                        "Температура старту нагрівача", "°C", settings?.heaterStartTemperatureC?.toDouble(),
                        0, 0.0, 100.0, true,
                    ) { onHeaterStartTemperature(it.toInt()) }
                }
            }

            item {
                Section("Ємність і комірки") {
                    EditableSettingRow(
                        "Максимальна ємність", "Аг", state.maxBatteryCapacityAh, 1, 0.0, 6500.0, false,
                    ) { onMaxBatteryCapacityAh(it) }
                    EditableSettingRow(
                        "Кількість комірок", "шт", state.cellCount?.toDouble(), 0, 0.0, 192.0, true,
                    ) { onTotalCellCount(it.toInt()) }
                    EditableSettingRow(
                        "Використана ємність", "Аг", state.usedCapacityAh, 0, 0.0, 6500.0, true,
                    ) { onUsedCapacityAh(it.toInt()) }
                    BooleanSettingRow(
                        "Автоскидання ємності", settings?.autoResetCapacity, "увімк.", "вимк.",
                        onOn = { onAutoResetCapacity(true) },
                        onOff = { onAutoResetCapacity(false) },
                    )
                }
            }

            item {
                Section("Затримки") {
                    EditableSettingRow(
                        "Затримка відключення хоста", "с", state.hostShutdownDelaySeconds?.toDouble(),
                        0, 1.0, 60000.0, true,
                    ) { onHostPowerOffDelaySec(it.toInt()) }
                    EditableSettingRow(
                        "Затримка передзаряду", "с", settings?.preChargeDelaySec?.toDouble(),
                        0, 0.0, 6500.0, true,
                    ) { onPreChargeDelaySec(it.toInt()) }
                }
            }

            item {
                Section("Канал за замовчуванням") {
                    BooleanSettingRow(
                        "Канал за замовчуванням", state.defaultChannelOn, "увімк.", "вимк.",
                        onOn = { onDefaultChannelState(true) },
                        onOff = { onDefaultChannelState(false) },
                    )
                }
            }

            item {
                Section("Інженерні (захищені паролем у штатному застосунку)") {
                    EditableSettingRow(
                        "🔧 Тип датчика струму", "", settings?.currentSensorType?.toDouble(), 0, 1.0, 3.0, true,
                    ) { onCurrentSensorType(it.toInt()) }
                    EditableSettingRow(
                        "🔧 CAN send ID", "", settings?.canSendId?.toDouble(), 0, 1.0, 32000.0, true,
                    ) { onCanSendId(it.toInt()) }
                    EditableSettingRow(
                        "🔧 CAN receive ID", "", settings?.canReceiveId?.toDouble(), 0, 1.0, 32000.0, true,
                    ) { onCanReceiveId(it.toInt()) }
                }
            }

            item {
                Section("Скидання") {
                    Text(
                        "Точна ціль кожної кнопки не підтверджена остаточно — можливо, дублюють одна одну.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorWarning,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onClearAction9) { Text("Скинути (0x09)") }
                        OutlinedButton(onClick = onResetDischargeCapacity) { Text("Скинути розряджену ємність (0x12)") }
                        OutlinedButton(onClick = onClearCycleCounter) { Text("Скинути цикли — CLR (0x13)") }
                    }
                }
            }
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
                when (currentValue) { true -> onLabel; false -> offLabel; null -> "—" },
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
            currentValue?.let { "%.${decimals}f %s".format(it, unit).trim() } ?: "— (тап, щоб задати)",
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
    var text by remember { mutableStateOf(initialValue?.let { "%.${decimals}f".format(it) } ?: "") }
    val parsed = text.toDoubleOrNull()
    val valid = parsed != null && parsed in min..max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column {
                if (unconfirmed) {
                    Text(
                        "Формула запису не підтверджена на пристрої — гіпотеза. Звірте показник після надсилання.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorWarning,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(if (unit.isBlank()) "значення" else unit) },
                    isError = !valid && text.isNotEmpty(),
                    singleLine = true,
                )
                Text(
                    "Межі: $min–$max ${unit.ifBlank { "" }}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            Button(onClick = { parsed?.let(onSubmit) }, enabled = valid) { Text("Надіслати") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        },
    )
}
