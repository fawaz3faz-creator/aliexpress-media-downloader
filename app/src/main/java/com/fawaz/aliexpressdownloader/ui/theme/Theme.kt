package com.fawaz.aliexpressdownloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8AB4F8),
    secondary = androidx.compose.ui.graphics.Color(0xFF5BBE99),
    tertiary = androidx.compose.ui.graphics.Color(0xFFB39DDB),
    background = androidx.compose.ui.graphics.Color(0xFF121212),
    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF121212),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E6E6),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E6E6)
)

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1A73E8),
    secondary = androidx.compose.ui.graphics.Color(0xFF2FAF7A),
    tertiary = androidx.compose.ui.graphics.Color(0xFF7E57C2),
    background = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1A1A)
)

@Composable
fun AliExpressMediaDownloaderTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
