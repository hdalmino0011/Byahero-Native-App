package com.hddev.byahero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hddev.byahero.R

val ByaheroCanvas = Color(0xFFF4F7F5)
val ByaheroGlassWhite = Color(0xD9FFFFFF)
val ByaheroSubtleGlass = Color(0xFFEBF1ED)
val ByaheroBlack = Color(0xFF101713)
val ByaheroSurface = Color(0xFFFFFFFF)
val ByaheroDarkGreen = Color(0xFF0F522B)
val ByaheroEmerald = Color(0xFF146C3A)
val ByaheroWhite = Color(0xFFFFFFFF)
val ByaheroMuted = Color(0xFF55625A)
val ByaheroBorder = Color(0xFFD6E2DA)

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold)
)

private val ByaheroColorScheme = lightColorScheme(
    primary = ByaheroDarkGreen,
    onPrimary = ByaheroWhite,
    secondary = ByaheroEmerald,
    onSecondary = ByaheroWhite,
    background = ByaheroCanvas,
    onBackground = ByaheroBlack,
    surface = ByaheroSurface,
    onSurface = ByaheroBlack,
    outline = ByaheroBorder
)

private val ByaheroTypography = Typography(
    displayLarge = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        letterSpacing = 1.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
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
