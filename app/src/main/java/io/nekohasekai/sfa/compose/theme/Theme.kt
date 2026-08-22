package io.nekohasekai.sfa.compose.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
        primary = GoolvpnCyan,
        onPrimary = Color(0xFF002B39),
        primaryContainer = Color(0xFF004E63),
        onPrimaryContainer = Color(0xFFB6EBFA),
        secondary = Color(0xFF82CEDF),
        onSecondary = Color(0xFF073640),
        secondaryContainer = Color(0xFF224A55),
        onSecondaryContainer = Color(0xFFC4EAF1),
        tertiary = Color(0xFF69D6AD),
        onTertiary = Color(0xFF003826),
        background = Color(0xFF07131F),
        onBackground = Color(0xFFDDE8ED),
        surface = Color(0xFF091927),
        onSurface = Color(0xFFDDE8ED),
        surfaceDim = Color(0xFF05101A),
        surfaceBright = Color(0xFF293946),
        surfaceContainerLowest = Color(0xFF040E17),
        surfaceContainerLow = Color(0xFF0C1C2A),
        surfaceContainer = Color(0xFF102231),
        surfaceContainerHigh = Color(0xFF172A39),
        surfaceContainerHighest = Color(0xFF203442),
        surfaceVariant = Color(0xFF24343D),
        onSurfaceVariant = Color(0xFFB7C9D1),
        surfaceTint = GoolvpnCyan,
        outline = Color(0xFF7F98A3),
        outlineVariant = Color(0xFF334A55),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF007C9E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC9F0FA),
        onPrimaryContainer = Color(0xFF003543),
        secondary = Color(0xFF3C6571),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD5EBF0),
        onSecondaryContainer = Color(0xFF17343C),
        tertiary = Color(0xFF167B5B),
        onTertiary = Color.White,
        background = Color(0xFFF4F7F9),
        onBackground = Color(0xFF142027),
        surface = Color(0xFFFBFDFE),
        onSurface = Color(0xFF142027),
        surfaceDim = Color(0xFFD7E0E4),
        surfaceBright = Color(0xFFFBFDFE),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF0F4F6),
        surfaceContainer = Color(0xFFEAF0F2),
        surfaceContainerHigh = Color(0xFFE4EBEE),
        surfaceContainerHighest = Color(0xFFDDE6EA),
        surfaceVariant = Color(0xFFDCE5E9),
        onSurfaceVariant = Color(0xFF3E5058),
        surfaceTint = Color(0xFF007C9E),
        outline = Color(0xFF6E818A),
        outlineVariant = Color(0xFFC4D0D5),
    )

@Composable
fun SFATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= 31 -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
