package com.cadeteria.cadete.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de marca CADEM (ver 1 logo.jpeg / 7 menu desplegable.jpeg en la raíz del repo):
// naranja vibrante + carbón oscuro, con blanco de fondo. Antes esta app usaba el azul de
// admin-front como placeholder — ahora que existe el logo real, la app de cadetes sigue
// la identidad real de la marca (admin-front queda pendiente de alinear en otro momento).
val CademOrange = Color(0xFFF26B1D)
val CademOrangeDark = Color(0xFFD65A12)
val CademOrangeContainerLight = Color(0xFFFFE4D1)
val CademCharcoal = Color(0xFF2B2B2B)
val CademCharcoalLight = Color(0xFF3D3D3D)

val Emerald600 = Color(0xFF059669)
val Emerald50 = Color(0xFFECFDF5)
val Amber500 = Color(0xFFF59E0B)
val Amber50 = Color(0xFFFFFBEB)
val Red600 = Color(0xFFDC2626)
val Red50 = Color(0xFFFEF2F2)

// Colores de acción del detalle de viaje: cada botón usa el color asociado a lo que hace
// (igual que el logo real de cada app) para que se distingan de un vistazo, no solo por el
// texto/emoji.
val CallBlue = Color(0xFF2563EB)
val WhatsappGreen = Color(0xFF25D366)
val MapsBlue = Color(0xFF1A73E8)
// Cyan de Waze un poco más oscuro que el de su logo: el original (0xFF33CCFF) es
// demasiado claro para texto/ícono blanco encima (falla contraste).
val WazeCyan = Color(0xFF0891B2)

val Gray50 = Color(0xFFFAFAFA)
val Gray100 = Color(0xFFF3F4F6)
val Gray300 = Color(0xFFD1D5DB)
// Un poco más oscuro que el gris "estándar" (0xFF6B7280) a propósito: este es el texto
// secundario (horarios, etiquetas) que un cadete tiene que poder leer de un vistazo con el
// sol pegando en la pantalla — más contraste contra blanco ayuda ahí (auditoría UX 2026-09-15).
val Gray500 = Color(0xFF57606A)
val Gray700 = Color(0xFF374151)
val Gray900 = Color(0xFF111827)
