package com.example.campusconnect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DominoColorScheme = lightColorScheme(
    primary = DominoCoral,
    onPrimary = DominoOnPrimary,
    primaryContainer = DominoCoralSoft,
    onPrimaryContainer = DominoCoralDeep,
    secondary = DominoPinkTint,
    onSecondary = DominoOnSurface,
    secondaryContainer = DominoCoralSoft,
    onSecondaryContainer = DominoCoralDeep,
    tertiary = DominoCoralDeep,
    onTertiary = DominoOnPrimary,
    background = DominoBackground,
    onBackground = DominoOnBackground,
    surface = DominoSurface,
    onSurface = DominoOnSurface,
    surfaceVariant = DominoSurfaceVariant,
    onSurfaceVariant = DominoOnSurfaceVariant,
    outline = DominoOutline,
    outlineVariant = DominoCoralSoft,
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun CampusConnectTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DominoColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
