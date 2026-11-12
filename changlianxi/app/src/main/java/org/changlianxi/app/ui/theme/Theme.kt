package org.changlianxi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF22754F),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F7F3),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFF7F7F3),
    onSurface = Color(0xFF1B1B1B),
    error = Color(0xFFB43F3B),
)

@Composable
fun ChanglianxiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}