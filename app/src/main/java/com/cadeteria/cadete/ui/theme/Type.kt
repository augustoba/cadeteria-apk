package com.cadeteria.cadete.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cadeteria.cadete.R

/**
 * Tipografías del sistema de diseño (spec-app-mejoras-visuales §1). Pesos recortados a los que
 * se usan de verdad (decisión 4): la app se reparte por Bluetooth y cada MB cuenta.
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

/** Solo para números (montos, contadores, estadísticas): hace que "$3.600" y "45" se lean como datos. */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/** Estilo para un número destacado — usar con `.copy(fontSize = …)` según el tamaño. */
val EstiloNumero = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)

private val base = Typography()

/** La escala de Material 3 de siempre, con Plus Jakarta Sans en todos los estilos. */
val CadeteTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = PlusJakartaSans),
    displayMedium = base.displayMedium.copy(fontFamily = PlusJakartaSans),
    displaySmall = base.displaySmall.copy(fontFamily = PlusJakartaSans),
    headlineLarge = base.headlineLarge.copy(fontFamily = PlusJakartaSans),
    headlineMedium = base.headlineMedium.copy(fontFamily = PlusJakartaSans),
    headlineSmall = base.headlineSmall.copy(fontFamily = PlusJakartaSans),
    titleLarge = base.titleLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = PlusJakartaSans),
    bodyMedium = base.bodyMedium.copy(fontFamily = PlusJakartaSans),
    bodySmall = base.bodySmall.copy(fontFamily = PlusJakartaSans),
    labelLarge = base.labelLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = PlusJakartaSans),
    labelSmall = base.labelSmall.copy(fontFamily = PlusJakartaSans),
)
