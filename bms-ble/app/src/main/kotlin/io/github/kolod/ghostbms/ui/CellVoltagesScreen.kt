package io.github.kolod.ghostbms.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import io.github.kolod.ghostbms.R
import io.github.kolod.ghostbms.ble.BmsState

/** Скільки комірок реально отримують живу напругу в одному банку цього протоколу. */
private const val PROTOCOL_VOLTAGE_CELL_LIMIT = 96

/**
 * Один температурний модуль обслуговує максимум 12 комірок; максимум 16 модулів
 * (16×12=192) покриває заявлену максимальну кількість комірок пристрою. Підтверджено
 * власником пристрою.
 */
private const val CELLS_PER_TEMP_MODULE = 12
private const val MAX_TEMP_MODULES = 16

/**
 * Скільки з очікуваних модулів мають підтверджене джерело даних у поточному
 * Kotlin-порту — сторінки 0x02+0x0A (`auxModuleTemperaturesC`), модулі 1-8. Модулі
 * 9-16 (сторінка 0x12) на пристрої власника стабільно нульові — не показуємо
 * (TODO, див. коментар `BmsState.moduleTemperaturesC`).
 */
private const val CONFIRMED_TEMP_MODULE_SOURCE_LIMIT = 8

/**
 * Таблиця напруг усіх комірок (аналог вікна4/窗口4 штатного застосунку). Протокол передає
 * напруги двома пакетами (BmsFrame.CellVoltages / AuxCellVoltages, нумерація вікон C2 і C4
 * декомпільованого коду), але комірки в пакеті можуть бути підключені у довільному порядку —
 * тож не групуємо їх візуально за джерелом пакета, а просто зводимо в одну послідовність
 * 1..2×perBank (перший пакет — позиції 1..perBank, другий — perBank+1..2×perBank) і показуємо
 * тільки ті, що вже мають ненульове значення, зберігаючи цю послідовність.
 */
@Composable
fun CellVoltagesScreen(
    state: BmsState,
    modifier: Modifier = Modifier,
) {
    val perBankCellCount = ((state.cellCount ?: (2 * PROTOCOL_VOLTAGE_CELL_LIMIT)) / 2)
        .coerceIn(0, PROTOCOL_VOLTAGE_CELL_LIMIT)

    val populatedCells = buildList {
        for (i in 1..perBankCellCount) {
            state.cellVoltages[i]?.takeIf { it != 0.0 }?.let { add(i to it) }
        }
        for (i in 1..perBankCellCount) {
            state.auxCellVoltages[i]?.takeIf { it != 0.0 }?.let { add((perBankCellCount + i) to it) }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(populatedCells, key = { it.first }) { (cellNumber, voltage) ->
            CellVoltageTile(cellNumber = cellNumber, voltage = voltage)
        }

        // Кількість модулів = скільки їх фізично потрібно для сконфігурованої кількості
        // комірок (1 модуль обслуговує максимум 12 комірок), а не просто "скільки
        // непорожніх ключів прийшло" — так грід одразу показує очікувану кількість
        // модулів, а не лише ті, що встигли надіслати дані.
        val totalCellCount = state.cellCount ?: (2 * PROTOCOL_VOLTAGE_CELL_LIMIT)
        val moduleCount = ceil(totalCellCount / CELLS_PER_TEMP_MODULE.toDouble()).toInt().coerceIn(0, MAX_TEMP_MODULES)
        if (moduleCount > 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.temp_modules_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            // Банк B (0x02+0x0A) — підтверджено, саме тут реальні дані з фізичних датчиків
            // для модулів 1-8. Банк A (0x12, state.moduleTemperaturesC) на пристрої
            // власника завжди нульовий, тож поки не показуємо — див. коментар полів у
            // BmsState.kt. Якщо конфігурація вимагає модулів понад 8-й — джерело для них
            // у протоколі ще не підтверджено (TODO), тож просто не рендеримо ці тайли.
            val shownModules = moduleCount.coerceAtMost(CONFIRMED_TEMP_MODULE_SOURCE_LIMIT)
            items(shownModules) { index ->
                val probe = index + 1
                TemperatureTile(probeNumber = probe, celsius = state.auxModuleTemperaturesC[probe])
            }
            if (moduleCount > CONFIRMED_TEMP_MODULE_SOURCE_LIMIT) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.temp_modules_unconfirmed, CONFIRMED_TEMP_MODULE_SOURCE_LIMIT + 1, moduleCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) { LastUpdatedRow(state.lastUpdated) }
    }
}

@Composable
private fun CellVoltageTile(cellNumber: Int, voltage: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.label_cell_number, cellNumber), style = MaterialTheme.typography.labelSmall)
            Text(
                "%.3f %s".format(voltage, stringResource(R.string.unit_v)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TemperatureTile(probeNumber: Int, celsius: Double?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.label_module_number, probeNumber), style = MaterialTheme.typography.labelSmall)
            Text(
                if (celsius == null) stringResource(R.string.value_missing) else "%.1f %s".format(celsius, stringResource(R.string.unit_c)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
