package com.yourapp.vault.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CryptoraThemeDefinition(
    val colorScheme: ColorScheme,
    val shapes: Shapes,
    val typography: Typography,
    val extraColors: ExtraColors
)

data class ExtraColors(
    val dialogBackground: Color,
    val dialogBorder: Color,
    val inputFieldBackground: Color,
    val inputFieldBorder: Color,
    val inputFieldCursor: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val fabBackground: Color,
    val fabIcon: Color,
    val snackbarBackground: Color,
    val snackbarText: Color,
    val switchChecked: Color,
    val switchUnchecked: Color,
    val navBarBackground: Color,
    val statusBarColor: Color
)

val BloodVaultTheme = CryptoraThemeDefinition(
    colorScheme = darkColorScheme(primary = Color(0xFFCC0000), onPrimary = Color.White, primaryContainer = Color(0xFF5C0000), secondary = Color(0xFF8B0000), background = Color(0xFF050000), surface = Color(0xFF110000), onBackground = Color(0xFFFFCDD2), onSurface = Color(0xFFFFB3B3), outline = Color(0xFF6B0000), error = Color(0xFFFF1744)),
    shapes = Shapes(small = CutCornerShape(4.dp), medium = CutCornerShape(0.dp), large = CutCornerShape(0.dp)),
    typography = Typography(headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp), bodyLarge = TextStyle(fontFamily = FontFamily.Serif)),
    extraColors = ExtraColors(Color(0xFF0F0000), Color(0xFFCC0000), Color(0xFF1A0000), Color(0xFF8B0000), Color(0xFFCC0000), Color(0xFF130000), Color(0xFF5C0000), Color(0xFFCC0000), Color.White, Color(0xFF2D0000), Color(0xFFFFCDD2), Color(0xFFCC0000), Color(0xFF3B0000), Color(0xFF050000), Color(0xFF050000))
)

val MatrixTheme = CryptoraThemeDefinition(
    colorScheme = darkColorScheme(primary = Color(0xFF00FF41), onPrimary = Color.Black, primaryContainer = Color(0xFF003B00), secondary = Color(0xFF008F11), background = Color.Black, surface = Color(0xFF050F05), onBackground = Color(0xFF00FF41), onSurface = Color(0xFF33FF57), outline = Color(0xFF005500), error = Color.Red),
    shapes = Shapes(small = RoundedCornerShape(0.dp), medium = RoundedCornerShape(0.dp), large = RoundedCornerShape(0.dp)),
    typography = Typography(headlineSmall = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp), bodyLarge = TextStyle(fontFamily = FontFamily.Monospace), labelLarge = TextStyle(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)),
    extraColors = ExtraColors(Color.Black, Color(0xFF00FF41), Color(0xFF001400), Color(0xFF00FF41), Color(0xFF00FF41), Color(0xFF050F05), Color(0xFF005500), Color(0xFF00FF41), Color.Black, Color(0xFF001A00), Color(0xFF00FF41), Color(0xFF00FF41), Color(0xFF003300), Color.Black, Color.Black)
)

val DeepOceanTheme = CryptoraThemeDefinition(
    colorScheme = darkColorScheme(primary = Color(0xFF00E5FF), onPrimary = Color.Black, primaryContainer = Color(0xFF00363D), secondary = Color(0xFF0097A7), background = Color(0xFF000A14), surface = Color(0xFF00131F), onBackground = Color(0xFFB2EBF2), onSurface = Color(0xFFCCF5FF), outline = Color(0xFF006978), error = Color(0xFFFF4081)),
    shapes = Shapes(small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp)),
    typography = Typography(headlineSmall = TextStyle(fontWeight = FontWeight.Light, fontSize = 28.sp), bodyLarge = TextStyle(fontSize = 16.sp)),
    extraColors = ExtraColors(Color(0xFF00131F), Color(0xFF00E5FF), Color(0xFF001E2E), Color(0xFF0097A7), Color(0xFF00E5FF), Color(0xFF00192A), Color(0xFF00363D), Color(0xFF00E5FF), Color.Black, Color(0xFF00272B), Color(0xFFB2EBF2), Color(0xFF00E5FF), Color(0xFF00363D), Color(0xFF000A14), Color(0xFF000A14))
)

val NuclearTheme = CryptoraThemeDefinition(
    colorScheme = darkColorScheme(primary = Color(0xFFCCFF00), onPrimary = Color.Black, primaryContainer = Color(0xFF2E3800), secondary = Color(0xFF8BC34A), background = Color(0xFF060800), surface = Color(0xFF0D1000), onBackground = Color(0xFFCCFF00), onSurface = Color(0xFFDDFF44), outline = Color(0xFF4A5C00), error = Color(0xFFFF6D00)),
    shapes = Shapes(small = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp), medium = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp), large = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)),
    typography = Typography(headlineSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp), bodyLarge = TextStyle(fontFamily = FontFamily.Monospace)),
    extraColors = ExtraColors(Color(0xFF0D1000), Color(0xFFCCFF00), Color(0xFF151C00), Color(0xFF8BC34A), Color(0xFFCCFF00), Color(0xFF101400), Color(0xFF2E3800), Color(0xFFCCFF00), Color.Black, Color(0xFF1A2200), Color(0xFFCCFF00), Color(0xFFCCFF00), Color(0xFF2E3800), Color(0xFF060800), Color(0xFF060800))
)

val InfernoTheme = CryptoraThemeDefinition(
    colorScheme = darkColorScheme(primary = Color(0xFFFF6600), onPrimary = Color.Black, primaryContainer = Color(0xFF7F2000), secondary = Color(0xFFFF3D00), background = Color(0xFF080200), surface = Color(0xFF130500), onBackground = Color(0xFFFFCCBC), onSurface = Color(0xFFFFBB99), outline = Color(0xFF7F2000), error = Color(0xFFFF1744)),
    shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp)),
    typography = Typography(headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.ExtraBold), bodyLarge = TextStyle(fontWeight = FontWeight.Medium)),
    extraColors = ExtraColors(Color(0xFF130500), Color(0xFFFF6600), Color(0xFF1F0800), Color(0xFF7F2000), Color(0xFFFF6600), Color(0xFF180600), Color(0xFF5C1000), Color(0xFFFF6600), Color.Black, Color(0xFF2D0A00), Color(0xFFFFCCBC), Color(0xFFFF6600), Color(0xFF5C1000), Color(0xFF080200), Color(0xFF080200))
)

val ChromePunkTheme = CryptoraThemeDefinition(
    colorScheme = darkColorScheme(primary = Color(0xFFB0BEC5), onPrimary = Color.Black, primaryContainer = Color(0xFF37474F), secondary = Color(0xFF78909C), background = Color(0xFF050608), surface = Color(0xFF0D0F12), onBackground = Color(0xFFB0BEC5), onSurface = Color(0xFFCFD8DC), outline = Color(0xFF455A64), error = Color(0xFFFF5252)),
    shapes = Shapes(small = RoundedCornerShape(topStart = 0.dp, topEnd = 6.dp, bottomStart = 6.dp, bottomEnd = 0.dp), medium = RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 0.dp), large = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 0.dp)),
    typography = Typography(headlineSmall = TextStyle(fontWeight = FontWeight.Thin, letterSpacing = 4.sp), bodyLarge = TextStyle(letterSpacing = 0.5.sp), labelLarge = TextStyle(letterSpacing = 2.sp)),
    extraColors = ExtraColors(Color(0xFF0D0F12), Color(0xFF78909C), Color(0xFF161B20), Color(0xFF455A64), Color(0xFFB0BEC5), Color(0xFF121518), Color(0xFF37474F), Color(0xFF78909C), Color.Black, Color(0xFF263238), Color(0xFFCFD8DC), Color(0xFFB0BEC5), Color(0xFF37474F), Color(0xFF050608), Color(0xFF050608))
)

val SakuraNoirTheme = CryptoraThemeDefinition(
    colorScheme = darkColorScheme(primary = Color(0xFFFF80AB), onPrimary = Color(0xFF1A0010), primaryContainer = Color(0xFF5C0030), secondary = Color(0xFFF48FB1), background = Color(0xFF0A0008), surface = Color(0xFF130010), onBackground = Color(0xFFFFD6E7), onSurface = Color(0xFFFFB3CC), outline = Color(0xFF7A0040), error = Color(0xFFFF5252)),
    shapes = Shapes(small = RoundedCornerShape(20.dp), medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp)),
    typography = Typography(headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light, letterSpacing = 2.sp), bodyLarge = TextStyle(fontFamily = FontFamily.Serif)),
    extraColors = ExtraColors(Color(0xFF130010), Color(0xFFFF80AB), Color(0xFF1F0018), Color(0xFF5C0030), Color(0xFFFF80AB), Color(0xFF180012), Color(0xFF3D0020), Color(0xFFFF80AB), Color(0xFF1A0010), Color(0xFF3D0020), Color(0xFFFFD6E7), Color(0xFFFF80AB), Color(0xFF5C0030), Color(0xFF0A0008), Color(0xFF0A0008))
)
