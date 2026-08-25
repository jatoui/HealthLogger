package com.jadatoui.healthlogger.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Seed = Color(0xFF1F6F5C)

private val LightColors = lightColorScheme(
    primary = Seed,
    secondary = Color(0xFF4B635B),
    tertiary = Color(0xFF9A5B2C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD9BF),
    secondary = Color(0xFFB2CCC1),
    tertiary = Color(0xFFF5B78A),
)

@Composable
fun HealthLoggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Material You on Android 12+, hand-picked palette below that.
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colors, content = content)
}
