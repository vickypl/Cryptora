package com.yourapp.vault.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BloodVaultColorScheme = darkColorScheme(
    primary = Color(0xFFB71C1C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7F0000),
    secondary = Color(0xFF880E4F),
    background = Color(0xFF0D0000),
    surface = Color(0xFF1A0000),
    onBackground = Color(0xFFFFCDD2),
    onSurface = Color(0xFFFFCDD2),
    error = Color(0xFFFF5252),
    outline = Color(0xFF8B0000)
)

private val MatrixColorScheme = darkColorScheme(
    primary = Color(0xFF00FF41),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003B00),
    secondary = Color(0xFF008F11),
    background = Color(0xFF000000),
    surface = Color(0xFF0D0D0D),
    onBackground = Color(0xFF00FF41),
    onSurface = Color(0xFF39FF14),
    error = Color(0xFFFF0000),
    outline = Color(0xFF005500)
)

private val DeepOceanColorScheme = darkColorScheme(
    primary = Color(0xFF00BCD4),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF006064),
    secondary = Color(0xFF0097A7),
    background = Color(0xFF000A14),
    surface = Color(0xFF001F2E),
    onBackground = Color(0xFFB2EBF2),
    onSurface = Color(0xFFE0F7FA),
    error = Color(0xFFFF1744),
    outline = Color(0xFF00838F)
)

private val NuclearColorScheme = darkColorScheme(
    primary = Color(0xFFCCFF00),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF3D5000),
    secondary = Color(0xFF8BC34A),
    background = Color(0xFF0A0C00),
    surface = Color(0xFF141A00),
    onBackground = Color(0xFFCCFF00),
    onSurface = Color(0xFFE6FF66),
    error = Color(0xFFFF6D00),
    outline = Color(0xFF558B2F)
)

private val InfernoColorScheme = darkColorScheme(
    primary = Color(0xFFFF6600),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF7F2000),
    secondary = Color(0xFFFF3D00),
    background = Color(0xFF0D0300),
    surface = Color(0xFF1A0800),
    onBackground = Color(0xFFFFCCBC),
    onSurface = Color(0xFFFFE0B2),
    error = Color(0xFFFF1744),
    outline = Color(0xFFBF360C)
)

@Composable
fun CryptoraTheme(
    selectedTheme: String = "MATRIX",
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        "BLOOD_VAULT" -> BloodVaultColorScheme
        "MATRIX" -> MatrixColorScheme
        "DEEP_OCEAN" -> DeepOceanColorScheme
        "NUCLEAR" -> NuclearColorScheme
        "INFERNO" -> InfernoColorScheme
        else -> MatrixColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

