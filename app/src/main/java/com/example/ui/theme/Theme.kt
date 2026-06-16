package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoPrimaryDark,
    onPrimary = BentoOnPrimaryDark,
    primaryContainer = BentoPrimaryContainerDark,
    onPrimaryContainer = BentoOnPrimaryContainerDark,
    background = BentoBgDark,
    onBackground = BentoOnBgDark,
    surface = BentoSurfaceDark,
    onSurface = BentoOnSurfaceDark,
    surfaceVariant = BentoSurfaceVariantDark,
    onSurfaceVariant = BentoOnSurfaceVariantDark,
    outline = BentoOutlineDark,
    outlineVariant = BentoOutlineDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoPrimaryLight,
    onPrimary = BentoOnPrimaryLight,
    primaryContainer = BentoPrimaryContainerLight,
    onPrimaryContainer = BentoOnPrimaryContainerLight,
    background = BentoBgLight,
    onBackground = BentoOnBgLight,
    surface = BentoSurfaceLight,
    onSurface = BentoOnSurfaceLight,
    surfaceVariant = BentoSurfaceVariantLight,
    onSurfaceVariant = BentoOnSurfaceVariantLight,
    outline = BentoOutlineLight,
    outlineVariant = BentoOutlineLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Override dynamic color to false so our brand palette remains consistent
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
