package com.mathcore.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mathcore.app.R

val SmallRadius  = 12.dp
val MediumRadius = 16.dp
val LargeRadius  = 24.dp

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

private val InterFontFamily = FontFamily(
    Font(GoogleFont("Inter"), fontProvider, FontWeight.Normal),
    Font(GoogleFont("Inter"), fontProvider, FontWeight.Medium),
    Font(GoogleFont("Inter"), fontProvider, FontWeight.SemiBold),
    Font(GoogleFont("Inter"), fontProvider, FontWeight.Bold),
    Font(GoogleFont("Inter"), fontProvider, FontWeight.ExtraBold)
)

private val InterTypography = Typography().run {
    copy(
        displayLarge  = displayLarge.copy(fontFamily  = InterFontFamily),
        displayMedium = displayMedium.copy(fontFamily = InterFontFamily),
        displaySmall  = displaySmall.copy(fontFamily  = InterFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = InterFontFamily),
        headlineMedium= headlineMedium.copy(fontFamily= InterFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = InterFontFamily),
        titleLarge    = titleLarge.copy(fontFamily    = InterFontFamily),
        titleMedium   = titleMedium.copy(fontFamily   = InterFontFamily),
        titleSmall    = titleSmall.copy(fontFamily    = InterFontFamily),
        bodyLarge     = bodyLarge.copy(fontFamily     = InterFontFamily),
        bodyMedium    = bodyMedium.copy(fontFamily    = InterFontFamily),
        bodySmall     = bodySmall.copy(fontFamily     = InterFontFamily),
        labelLarge    = labelLarge.copy(fontFamily    = InterFontFamily),
        labelMedium   = labelMedium.copy(fontFamily   = InterFontFamily),
        labelSmall    = labelSmall.copy(fontFamily    = InterFontFamily)
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEEAFF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFFBEB),
    onTertiaryContainer = Color(0xFF92400E),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurface = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF001A41),
    primaryContainer = Color(0xFF1D3A6E),
    onPrimaryContainer = Color(0xFFDEEAFF),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF003D2A),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF2D1B00),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF243248),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

@Composable
fun MathCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = InterTypography,
        content = content
    )
}
