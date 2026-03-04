package com.yourapp.vault.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun CryptoraTheme(
    selectedTheme: String = "MATRIX",
    content: @Composable () -> Unit
) {
    val definition = when (selectedTheme) {
        "BLOOD_VAULT" -> BloodVaultTheme
        "MATRIX" -> MatrixTheme
        "DEEP_OCEAN" -> DeepOceanTheme
        "NUCLEAR" -> NuclearTheme
        "INFERNO" -> InfernoTheme
        "CHROME_PUNK" -> ChromePunkTheme
        "SAKURA_NOIR" -> SakuraNoirTheme
        else -> MatrixTheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = definition.extraColors.statusBarColor.toArgb()
            window.navigationBarColor = definition.extraColors.navBarBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(LocalExtraColors provides definition.extraColors) {
        MaterialTheme(
            colorScheme = definition.colorScheme,
            shapes = definition.shapes,
            typography = definition.typography,
            content = content
        )
    }
}
