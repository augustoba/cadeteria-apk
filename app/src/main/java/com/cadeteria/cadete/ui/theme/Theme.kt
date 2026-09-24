package com.cadeteria.cadete.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = CademOrange,
    onPrimary = Color.White,
    primaryContainer = CademOrangeContainerLight,
    onPrimaryContainer = CademOrangeDark,
    secondary = CademCharcoal,
    onSecondary = Color.White,
    secondaryContainer = CademOrangeContainerLight,
    onSecondaryContainer = CademOrangeDark,
    tertiary = Emerald600,
    error = Red600,
    background = Gray50,
    onBackground = Gray900,
    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray300,
)

private val DarkSurface = Color(0xFF1E1E1E)
private val DarkSurfaceVariant = Color(0xFF333333)
private val DarkOnSurfaceVariant = Color(0xFFCACACA)

private val DarkColors = darkColorScheme(
    primary = CademOrange,
    onPrimary = Color.White,
    primaryContainer = CademOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = CademCharcoalLight,
    onSecondary = Color.White,
    secondaryContainer = CademCharcoalLight,
    onSecondaryContainer = CademOrangeContainerLight,
    tertiary = Emerald600,
    error = Red600,
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = DarkSurface,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF5C5C5C),
)

private val CadeteShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CadeteAppTheme(
    tema: TemaApp = TemaApp.SISTEMA,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (tema) {
        TemaApp.OSCURO -> true
        TemaApp.CLARO -> false
        TemaApp.SISTEMA -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = CadeteTypography, shapes = CadeteShapes, content = content)
}
