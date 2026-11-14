package com.example.foundit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Brand colors
private val Navy = Color(0xFF1B3A6B)
private val NavyLight = Color(0xFFA9C7FF)

// Status colors (shared by light and dark; chosen for readable contrast on their containers)
object StatusColors {
    val Lost = Color(0xFFB45309)        // amber-700
    val LostContainer = Color(0xFFFFEDD5)
    val Found = Color(0xFF1D4ED8)       // blue-700
    val FoundContainer = Color(0xFFDBEAFE)
    val Claimed = Color(0xFF047857)     // emerald-700
    val ClaimedContainer = Color(0xFFD1FAE5)
}

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF555F71),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFF8F9FC),
    surfaceVariant = Color(0xFFE1E2EC),
    surfaceContainer = Color(0xFFEDEEF4),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = NavyLight,
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF00458E),
    onPrimaryContainer = Color(0xFFD6E3FF),
    background = Color(0xFF111318),
    surface = Color(0xFF111318),
    error = Color(0xFFFFB4AB),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun FoundItTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        content = content
    )
}
