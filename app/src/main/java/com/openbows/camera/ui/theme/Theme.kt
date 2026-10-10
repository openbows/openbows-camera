package com.openbows.camera.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Gold = Color(0xFFF5C15C)
private val DarkBg = Color(0xFF101216)
private val Surface = Color(0xFF1B1F24)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1B1F24),
    background = DarkBg,
    surface = Surface,
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5),
    secondary = Color(0xFF8AB4F8),
)

@Composable
fun OpenbowsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
