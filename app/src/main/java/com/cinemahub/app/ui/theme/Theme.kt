package com.cinemahub.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9B8CFF),
    secondary = Color(0xFFC3B9FF),
    background = Color(0xFF0D1016),
    surface = Color(0xFF171C25)
)

@Composable
fun CinemaHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
