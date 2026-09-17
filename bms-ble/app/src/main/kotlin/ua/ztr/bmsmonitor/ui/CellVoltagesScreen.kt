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

/**
 * Таблиця напруг усіх комірок (аналог вікна4/窗口4 штатного застосунку). Штатний
 * застосунок сам ніде не показує живу напругу для комірок >96 — цей протокол їх
 * не передає (лише прапорці балансування), тож сітка обмежена 96 комірками.
 */
@Composable
fun CellVoltagesScreen(
    state: BmsState,
    modifier: Modifier = Modifier,
) {
    val cellCount = (state.cellCount ?: 96).coerceIn(0, 96)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(cellCount) { index ->
            val cellNumber = index + 1
            CellVoltageTile(cellNumber, state.cellVoltages[cellNumber])
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
