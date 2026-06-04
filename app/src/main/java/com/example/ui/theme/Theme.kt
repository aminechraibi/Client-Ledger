package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA6C8FF),
    onPrimary = Color(0xFF003060),
    primaryContainer = Color(0xFF004786),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFC1C6D2),
    onSecondary = Color(0xFF2B303A),
    secondaryContainer = Color(0xFF414751),
    onSecondaryContainer = Color(0xFFE1E2EC),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF2B303A),
    onSurfaceVariant = Color(0xFFC1C6D2),
    outlineVariant = Color(0xFF414751)
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalBluePrimary,
    onPrimary = MinimalBlueOnPrimary,
    primaryContainer = MinimalBlueContainer,
    onPrimaryContainer = MinimalBlueOnContainer,
    secondary = MinimalNeutralOnContainer,
    onSecondary = MinimalNeutralContainer,
    secondaryContainer = MinimalNeutralContainer,
    onSecondaryContainer = MinimalNeutralOnContainer,
    background = MinimalBackground,
    onBackground = MinimalOnBackground,
    surface = MinimalSurface,
    onSurface = MinimalOnSurface,
    surfaceVariant = MinimalSurfaceVariant,
    onSurfaceVariant = MinimalOnSurfaceVariant,
    outlineVariant = MinimalOutlineVariant,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
