package com.cadeteria.cadete.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.cadeteria.cadete.ui.theme.CademOrange

/**
 * Isotipo de texto de la marca (ver 1 logo.jpeg en la raíz del repo): "CAD" en naranja +
 * "EM" en el color de contraste que corresponda según el fondo (blanco sobre carbón,
 * carbón sobre blanco). No es una imagen — se arma con texto para no depender de un
 * asset de diseño y escalar sin pérdida en cualquier densidad.
 */
@Composable
fun CademWordmark(
    modifier: Modifier = Modifier,
    contraste: Color,
    tamano: TextUnit = 32.sp,
) {
    Row(modifier) {
        Text("CAD", color = CademOrange, fontWeight = FontWeight.Black, fontSize = tamano)
        Text("EM", color = contraste, fontWeight = FontWeight.Black, fontSize = tamano)
    }
}
