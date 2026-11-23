package com.whispervolume.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object WhisperPalette {
    val Background = Color(0xFF080C0D)
    val BackgroundSoft = Color(0xFF0D1415)
    val Surface = Color(0xFF111A19)
    val SurfaceHigh = Color(0xFF172321)
    val Accent = Color(0xFFA8E8CC)
    val AccentDeep = Color(0xFF6FA88F)
    val Text = Color(0xFFF2F6F3)
    val TextMuted = Color(0xFF8CA099)
    val TextFaint = Color(0xFF5D716A)
    val Line = Color(0x1FFFFFFF)
    val Warm = Color(0xFFF0C792)
    val Error = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF3B1D1B)
}

private val WhisperColors = darkColorScheme(
    primary = WhisperPalette.Accent,
    onPrimary = Color(0xFF07110D),
    primaryContainer = Color(0xFF1E3A30),
    onPrimaryContainer = WhisperPalette.Accent,
    secondary = WhisperPalette.Warm,
    onSecondary = Color(0xFF1D140A),
    background = WhisperPalette.Background,
    onBackground = WhisperPalette.Text,
    surface = WhisperPalette.Surface,
    onSurface = WhisperPalette.Text,
    surfaceVariant = WhisperPalette.SurfaceHigh,
    onSurfaceVariant = WhisperPalette.TextMuted,
    outline = Color(0xFF31413C),
    outlineVariant = WhisperPalette.Line,
    error = WhisperPalette.Error,
    onError = Color(0xFF2A0907),
    errorContainer = WhisperPalette.ErrorContainer,
    onErrorContainer = Color(0xFFFFDAD5),
)

private val WhisperType = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 58.sp,
        lineHeight = 62.sp,
        letterSpacing = (-2.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.8.sp,
    ),
)

@Composable
fun WhisperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WhisperColors,
        typography = WhisperType,
        content = content,
    )
}
