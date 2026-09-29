package com.fayroz.requests.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NavyDeep = Color(0xFF04131D)
private val Navy = Color(0xFF071E2C)
private val NavySoft = Color(0xFF0D2D3E)
private val NavyLifted = Color(0xFF123848)
private val Turquoise = Color(0xFF22CDB9)
private val TurquoiseBright = Color(0xFF68E6D7)
private val TurquoiseContainer = Color(0xFFD9F6F1)
private val Gold = Color(0xFFD6A84B)
private val GoldLight = Color(0xFFF0D38D)
private val BackgroundLight = Color(0xFFF4F7F8)
private val SurfaceLight = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF142229)
private val MutedDark = Color(0xFF607780)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = TurquoiseContainer,
    onPrimaryContainer = Color(0xFF0C5A50),
    secondary = Turquoise,
    onSecondary = NavyDeep,
    secondaryContainer = Color(0xFFE4F8F5),
    onSecondaryContainer = Color(0xFF0B5E54),
    tertiary = Gold,
    onTertiary = NavyDeep,
    tertiaryContainer = Color(0xFFFFF1CE),
    onTertiaryContainer = Color(0xFF5A4212),
    background = BackgroundLight,
    onBackground = TextDark,
    surface = SurfaceLight,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFEAF0F2),
    onSurfaceVariant = MutedDark,
    outline = Color(0xFFA8B7BE),
    outlineVariant = Color(0xFFD5E0E4),
)

private val DarkColors = darkColorScheme(
    primary = Turquoise,
    onPrimary = NavyDeep,
    primaryContainer = Color(0xFF0B3A3A),
    onPrimaryContainer = TurquoiseBright,
    secondary = Gold,
    onSecondary = NavyDeep,
    secondaryContainer = Color(0xFF382C18),
    onSecondaryContainer = GoldLight,
    tertiary = Color(0xFF56D6AF),
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = Color(0xFFF3F7F8),
    surface = Color(0xFF091F2B),
    onSurface = Color(0xFFF3F7F8),
    surfaceVariant = NavySoft,
    onSurfaceVariant = Color(0xFF9CB1BB),
    outline = Color(0xFF416070),
    outlineVariant = Color(0xFF203E4D),
)

private val Type = Typography(
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
)

private val FayrozShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun FayrozTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Type,
        shapes = FayrozShapes,
        content = content,
    )
}
