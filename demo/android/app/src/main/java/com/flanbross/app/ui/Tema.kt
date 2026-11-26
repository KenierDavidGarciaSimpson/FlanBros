package com.flanbross.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Esquema = lightColorScheme(
    primary = Color(0xFFC2185B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E3),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF8D6E63),
    secondaryContainer = Color(0xFFFFE9D6),
    onSecondaryContainer = Color(0xFF3B2416),
    background = Color(0xFFFFF8F4),
    surface = Color(0xFFFFFBF8),
)

@Composable
fun FlanBrossTema(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, content = contenido)
}
