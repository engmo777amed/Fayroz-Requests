package com.fayroz.requests.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF0E2433)
private val Navy2 = Color(0xFF15384C)
private val Turquoise = Color(0xFF18B7A0)
private val Gold = Color(0xFFD6A84B)
private val SoftBackground = Color(0xFFF4F7F8)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Turquoise,
    tertiary = Gold,
    background = SoftBackground,
    surface = Color.White,
    onSurface = Color(0xFF172126),
)

private val DarkColors = darkColorScheme(
    primary = Turquoise,
    secondary = Gold,
    tertiary = Color(0xFF7AD8CB),
    background = Color(0xFF091820),
    surface = Navy,
    surfaceVariant = Navy2,
    onSurface = Color(0xFFE7F0F3),
)

@Composable
fun FayrozTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
