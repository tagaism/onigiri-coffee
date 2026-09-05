package com.onigiri.spend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Cream = Color(0xFFFFF8F0)
private val Paper = Color(0xFFFFF1E3)
private val Espresso = Color(0xFF3B2417)
private val Terracotta = Color(0xFFC45C26)
private val Moss = Color(0xFF3F6B4B)

private val Colors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    secondary = Moss,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Espresso,
    surface = Color.White,
    onSurface = Espresso,
    surfaceVariant = Paper,
    onSurfaceVariant = Color(0xFF6B4E3D),
    error = Color(0xFFB3261E),
)

@Composable
fun OnigiriTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
