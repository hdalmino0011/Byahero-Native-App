package com.hddev.byahero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ByaheroCanvas = Color(0xFFF4F7F5)
val ByaheroGlassWhite = Color(0xCCFFFFFF)
val ByaheroBlack = Color(0xFF101713)
val ByaheroSurface = Color(0xFFFFFFFF)
val ByaheroDarkGreen = Color(0xFF0F522B)
val ByaheroEmerald = Color(0xFF146C3A)
val ByaheroWhite = Color(0xFFFFFFFF)
val ByaheroMuted = Color(0xFF55625A)

// Drop poppins_regular.ttf / poppins_bold.ttf in res/font/ to bind Poppins directly
val PoppinsFontFamily = FontFamily.SansSerif

private val ByaheroColorScheme = lightColorScheme(
    primary = ByaheroDarkGreen,
    onPrimary = ByaheroWhite,
    secondary = ByaheroEmerald,
    onSecondary = ByaheroWhite,
    background = ByaheroCanvas,
    onBackground = ByaheroBlack,
    surface = ByaheroSurface,
    onSurface = ByaheroBlack
)

private val ByaheroTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = 2.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    )
)

@Composable
fun ByaheroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ByaheroColorScheme,
        typography = ByaheroTypography,
        content = content
    )
}
