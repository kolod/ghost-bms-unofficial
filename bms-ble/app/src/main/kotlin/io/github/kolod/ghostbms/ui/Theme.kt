package io.github.kolod.ghostbms.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenPrimary = Color(0xFF2E7D32)
private val GreenPrimaryDark = Color(0xFF81C784)
private val AmberWarning = Color(0xFFFFA000)
private val RedAlarm = Color(0xFFD32F2F)

val ColorAlarm = RedAlarm
val ColorWarning = AmberWarning
val ColorOk = GreenPrimary

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    secondary = AmberWarning,
    error = RedAlarm,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    secondary = AmberWarning,
    error = RedAlarm,
)

@Composable
fun BmsMonitorTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
