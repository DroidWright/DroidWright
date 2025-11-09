/**
 * Copyright (c) 2025 tas33n
 *
 * Licensed under the MIT License
 * See LICENSE file or https://opensource.org/licenses/MIT
 *
 * @author tas33n
 * @see <a href="https://github.com/tas33n/droidwright">GitHub</a>
 */
package com.tas33n.droidwright.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1f88e5),
    onPrimary = Color(0xFFffffff),
    primaryContainer = Color(0xFFd5e3ff),
    onPrimaryContainer = Color(0xFF001a4d),
    secondary = Color(0xFF5b5f7e),
    onSecondary = Color(0xFFffffff),
    secondaryContainer = Color(0xFFdfe2f7),
    onSecondaryContainer = Color(0xFF17193e),
    tertiary = Color(0xFF765a53),
    onTertiary = Color(0xFFffffff),
    tertiaryContainer = Color(0xFFffddd5),
    onTertiaryContainer = Color(0xFF2c1410),
    error = Color(0xFFb3261e),
    onError = Color(0xFFffffff),
    errorContainer = Color(0xFFf9dedc),
    onErrorContainer = Color(0xFF410e0b),
    background = Color(0xFFfffbfe),
    onBackground = Color(0xFF1c1b1f),
    surface = Color(0xFFfffbfe),
    onSurface = Color(0xFF1c1b1f),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFa8c7ff),
    onPrimary = Color(0xFF003087),
    primaryContainer = Color(0xFF0050bf),
    onPrimaryContainer = Color(0xFFd5e3ff),
    secondary = Color(0xFFc1c7e0),
    onSecondary = Color(0xFF2e3154),
    secondaryContainer = Color(0xFF44476b),
    onSecondaryContainer = Color(0xFFdfe2f7),
    tertiary = Color(0xFFf0bdb0),
    onTertiary = Color(0xFF462621),
    tertiaryContainer = Color(0xFF603836),
    onTertiaryContainer = Color(0xFFffddd5),
    error = Color(0xFFf2b8b5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8c1d18),
    onErrorContainer = Color(0xFFf9dedc),
    background = Color(0xFF1c1b1f),
    onBackground = Color(0xFFe6e1e6),
    surface = Color(0xFF1c1b1f),
    onSurface = Color(0xFFe6e1e6),
)

@Composable
fun UIAutomatorAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
