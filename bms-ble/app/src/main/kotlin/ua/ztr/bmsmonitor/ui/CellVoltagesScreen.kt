package ua.ztr.bmsmonitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.ztr.bmsble.BmsState

/** Максимальна кількість комірок, яку теоретично підтримує BMS (за інструкцією пристрою). */
private const val MAX_CELLS = 192

/** Скільки комірок реально отримують живу напругу в цьому протоколі (перевірено по обох вікнах штатного застосунку). */
private const val PROTOCOL_VOLTAGE_CELL_LIMIT = 96

/**
 * Таблиця напруг усіх комірок (аналог вікна4/窗口4 штатного застосунку). Сітка
 * розрахована на до 192 комірок (`state.cellCount`, за інструкцією пристрою), але
 * BLE-протокол цього застосунку (перевірено в обох вікнах реального часу штатного
 * застосунку — C2.java і C4.java) реально передає живу напругу лише для перших 96 —
 * решта показуються як "н/д". Ймовірна причина: власний екран BMS перемикає групи
 * по 48 комірок (літера A/B/C/D у кутку) — можливо, комірки 97-192 потребують ще
 * не знайденої команди "перемкнути групу" (кандидати — команди 0x09/0x12/0x13,
 * чия точна мета не підтверджена, див. format.md).
 */
@Composable
fun CellVoltagesScreen(
    state: BmsState,
    modifier: Modifier = Modifier,
) {
    val cellCount = (state.cellCount ?: PROTOCOL_VOLTAGE_CELL_LIMIT).coerceIn(0, MAX_CELLS)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(cellCount) { index ->
            val cellNumber = index + 1
            CellVoltageTile(
                cellNumber = cellNumber,
                voltage = state.cellVoltages[cellNumber],
                outOfProtocolRange = cellNumber > PROTOCOL_VOLTAGE_CELL_LIMIT,
            )
        }
        state.moduleTemperaturesC?.let { temps ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Section("Температура модулів") {
                    Column {
                        temps.forEachIndexed { i, t ->
                            MetricRow("Модуль ${i + 1}", "%.1f °C".format(t))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellVoltageTile(cellNumber: Int, voltage: Double?, outOfProtocolRange: Boolean) {
    val missing = voltage == null || voltage == 0.0
    val color = if (missing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("№$cellNumber", style = MaterialTheme.typography.labelSmall)
            Text(
                when {
                    outOfProtocolRange -> "н/д"
                    missing -> "—"
                    else -> "%.3f В".format(voltage)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}
