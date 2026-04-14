package cn.ksuser.auth.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimaryDark,
    onPrimary = GoldOnPrimaryDark,
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
    onSurface = GoldOnSurfaceDark,
    surfaceVariant = GoldSurfaceVariantDark,
    onSurfaceVariant = GoldOnSurfaceVariantDark,
    outline = GoldOutlineDark,
    error = GoldErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = GoldPrimaryLight,
    onPrimary = GoldOnPrimaryLight,
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
    onSurface = GoldOnSurfaceLight,
    surfaceVariant = GoldSurfaceVariantLight,
    onSurfaceVariant = GoldOnSurfaceVariantLight,
    outline = GoldOutlineLight,
    error = GoldErrorLight,
)

@Composable
fun KsuserAuthAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
        content = content
    )
}
