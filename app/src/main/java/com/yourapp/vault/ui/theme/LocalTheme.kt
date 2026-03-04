package com.yourapp.vault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalExtraColors = staticCompositionLocalOf { MatrixTheme.extraColors }

val MaterialTheme.extraColors: ExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtraColors.current
