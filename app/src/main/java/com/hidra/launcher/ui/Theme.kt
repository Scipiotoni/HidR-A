package com.hidra.launcher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF818CF8)
private val IndigoDark = Color(0xFF6366F1)

private val DarkColors = darkColorScheme(
    primary = Indigo,
    secondary = Color(0xFFA5B4FC),
    background = Color(0xFF0B1020),
    surface = Color(0xFF151A2E),
)

private val LightColors = lightColorScheme(
    primary = IndigoDark,
    secondary = Color(0xFF4F46E5),
)

@Composable
fun HidrATheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
