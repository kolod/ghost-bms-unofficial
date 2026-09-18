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

/** Скільки комірок реально отримують живу напругу в одному банку цього протоколу. */
private const val PROTOCOL_VOLTAGE_CELL_LIMIT = 96

/**
 * Таблиця напруг усіх комірок (аналог вікна4/窗口4 штатного застосунку). 192-комірковий
 * пакет складається з ДВОХ незалежних банків по 96 комірок кожен (BLE-протокол передає
 * банки під різними pageType-схемами — нумерація вікон C2 і C4 декомпільованого коду).
 * Номер комірки в банку A і той самий номер у банку B — це РІЗНІ фізичні комірки, тож
 * показуємо банки окремими сітками, кожна з власною нумерацією 1..96.
 */
@Composable
fun CellVoltagesScreen(
    state: BmsState,
    modifier: Modifier = Modifier,
) {
    val perBankCellCount = ((state.cellCount ?: (2 * PROTOCOL_VOLTAGE_CELL_LIMIT)) / 2)
        .coerceIn(0, PROTOCOL_VOLTAGE_CELL_LIMIT)

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Банк A",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        items(perBankCellCount) { index ->
            val cellNumber = index + 1
            CellVoltageTile(cellNumber = cellNumber, voltage = state.cellVoltages[cellNumber])
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Банк B",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(perBankCellCount) { index ->
            val cellNumber = index + 1
            CellVoltageTile(cellNumber = cellNumber, voltage = state.auxCellVoltages[cellNumber])
        }

        // Банк B температур (0x02+0x0A) — підтверджено, саме тут реальні дані з фізичних
        // датчиків. Банк A (0x12, state.moduleTemperaturesC) на пристрої власника завжди
        // нульовий, тож поки не показуємо — див. коментар полів у BmsState.kt.
        if (state.auxModuleTemperaturesC.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Температура модулів",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            val temps = state.auxModuleTemperaturesC.toSortedMap()
            items(temps.entries.toList()) { (probe, celsius) ->
                TemperatureTile(probeNumber = probe, celsius = celsius)
            }
        }
    }
}

@Composable
private fun CellVoltageTile(cellNumber: Int, voltage: Double?) {
    val missing = voltage == null || voltage == 0.0
    val color = if (missing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("№$cellNumber", style = MaterialTheme.typography.labelSmall)
            Text(
                if (missing) "—" else "%.3f В".format(voltage),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

@Composable
private fun TemperatureTile(probeNumber: Int, celsius: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Модуль $probeNumber", style = MaterialTheme.typography.labelSmall)
            Text(
                "%.1f °C".format(celsius),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
