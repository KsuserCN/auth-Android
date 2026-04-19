package cn.ksuser.auth.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimaryDark,
    onPrimary = GoldOnPrimaryDark,
    surfaceTint = GoldPrimaryDark,
    primaryContainer = GoldPrimaryContainerDark,
    onPrimaryContainer = GoldOnPrimaryContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldSecondaryContainerDark,
    onSecondaryContainer = GoldOnSecondaryContainerDark,
    tertiary = GoldTertiaryDark,
    onTertiary = GoldOnTertiaryDark,
    background = GoldBackgroundDark,
    onBackground = GoldOnSurfaceDark,
    surface = GoldSurfaceDark,
    surfaceDim = GoldSurfaceDimDark,
    surfaceBright = GoldSurfaceBrightDark,
    surfaceContainerLowest = GoldSurfaceContainerLowestDark,
    surfaceContainerLow = GoldSurfaceContainerLowDark,
    surfaceContainer = GoldSurfaceContainerDark,
    surfaceContainerHigh = GoldSurfaceContainerHighDark,
    surfaceContainerHighest = GoldSurfaceContainerHighestDark,
    onSurface = GoldOnSurfaceDark,
    surfaceVariant = GoldSurfaceVariantDark,
    onSurfaceVariant = GoldOnSurfaceVariantDark,
    outline = GoldOutlineDark,
    outlineVariant = GoldOutlineVariantDark,
    error = GoldErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = GoldPrimaryLight,
    onPrimary = GoldOnPrimaryLight,
    surfaceTint = GoldPrimaryLight,
    primaryContainer = GoldPrimaryContainerLight,
    onPrimaryContainer = GoldOnPrimaryContainerLight,
    secondary = GoldSecondaryLight,
    onSecondary = GoldOnSecondaryLight,
    secondaryContainer = GoldSecondaryContainerLight,
    onSecondaryContainer = GoldOnSecondaryContainerLight,
    tertiary = GoldTertiaryLight,
    onTertiary = GoldOnTertiaryLight,
    background = GoldBackgroundLight,
    onBackground = GoldOnSurfaceLight,
    surface = GoldSurfaceLight,
    surfaceDim = GoldSurfaceDimLight,
    surfaceBright = GoldSurfaceBrightLight,
    surfaceContainerLowest = GoldSurfaceContainerLowestLight,
    surfaceContainerLow = GoldSurfaceContainerLowLight,
    surfaceContainer = GoldSurfaceContainerLight,
    surfaceContainerHigh = GoldSurfaceContainerHighLight,
    surfaceContainerHighest = GoldSurfaceContainerHighestLight,
    onSurface = GoldOnSurfaceLight,
    surfaceVariant = GoldSurfaceVariantLight,
    onSurfaceVariant = GoldOnSurfaceVariantLight,
    outline = GoldOutlineLight,
    outlineVariant = GoldOutlineVariantLight,
    error = GoldErrorLight,
)

@Composable
fun KsuserAuthAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun rememberAppBackgroundBrush(): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val darkTheme = isSystemInDarkTheme()
    return remember(colorScheme, darkTheme) {
        val colors = if (darkTheme) {
            listOf(
                colorScheme.surfaceDim,
                colorScheme.background,
                colorScheme.surfaceContainerLow,
            )
        } else {
            listOf(
                colorScheme.surfaceBright,
                colorScheme.background,
                colorScheme.surfaceContainerLow,
            )
        }
        Brush.verticalGradient(colors)
    }
}
