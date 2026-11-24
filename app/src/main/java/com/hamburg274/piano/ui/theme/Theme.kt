package com.hamburg274.piano.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HamburgDarkColorScheme = darkColorScheme(
    primary = ConcertBrass,
    onPrimary = ObsidianBg,
    primaryContainer = SurfaceSelected,
    onPrimaryContainer = ConcertBrassBright,
    secondary = LeftHandBlue,
    onSecondary = IvoryText,
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = SurfaceCard,
    onSurface = IvoryText,
    surfaceVariant = SurfaceControl,
    onSurfaceVariant = MutedStoneText,
    outline = BorderHairline,
    error = RecordCrimsonLight,
    onError = ObsidianBg
)

@Composable
fun Hamburg274Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HamburgDarkColorScheme,
        typography = HamburgTypography,
        content = content
    )
}
