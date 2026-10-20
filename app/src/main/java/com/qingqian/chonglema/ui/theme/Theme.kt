package com.qingqian.chonglema.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** 夜间配色（原来的深色主题）。 */
private val NightColors = darkColorScheme(
    primary = Color(0xFFFF5C8A),
    onPrimary = Color(0xFF36000F),
    primaryContainer = Color(0xFF5C122F),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFC49BFF),
    onSecondary = Color(0xFF23103F),
    secondaryContainer = Color(0xFF3A2455),
    onSecondaryContainer = Color(0xFFEBD9FF),
    tertiary = Color(0xFF6FD6FF),
    onTertiary = Color(0xFF00313F),
    background = Color(0xFF100E12),
    onBackground = Color(0xFFF3E9EE),
    surface = Color(0xFF16131A),
    onSurface = Color(0xFFF3E9EE),
    // 夜间把次要文字 / 分隔线 / 卡片底色都提亮一档，深色下更容易分清层次
    surfaceVariant = Color(0xFF2A2430),
    onSurfaceVariant = Color(0xFFD8CCD6),
    surfaceContainer = Color(0xFF1D1922),
    surfaceContainerHigh = Color(0xFF28222E),
    outline = Color(0xFF6B5E70),
    outlineVariant = Color(0xFF342C3A),
)

/** 白天配色：蓝色系亮色主题。 */
private val DayColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF4F5BD5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E5FF),
    onSecondaryContainer = Color(0xFF0E1650),
    tertiary = Color(0xFF0E7490),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F8FF),
    onBackground = Color(0xFF0F1B2D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1B2D),
    surfaceVariant = Color(0xFFE2EAF6),
    onSurfaceVariant = Color(0xFF44566F),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEAF2FE),
    outline = Color(0xFF8496AE),
    outlineVariant = Color(0xFFD4DFED),
)

/** 当前是否夜间主题，供需要自定义配色的组件（如首页渐变卡）读取。 */
val LocalDarkTheme = staticCompositionLocalOf { true }

/**
 * 应用主题。
 * @param darkTheme true = 夜间配色，false = 白天配色。
 */
@Composable
fun ChonglemaTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = remember(darkTheme) { if (darkTheme) NightColors else DayColors }
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        SideEffect {
            val window = activity?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
