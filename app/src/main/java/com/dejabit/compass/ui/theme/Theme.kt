package com.dejabit.compass.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val CompassColorPalette = Colors(
    primary = TrueNorthAccent,
    primaryVariant = MagneticNorthAccent,
    secondary = SouthAccent,
    background = BackgroundBlack,
    surface = BackgroundBlack,
    onPrimary = BackgroundBlack,
    onBackground = DialCardinalText,
    onSurface = DialCardinalText
)

@Composable
fun CompassTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = CompassColorPalette,
        content = content
    )
}
