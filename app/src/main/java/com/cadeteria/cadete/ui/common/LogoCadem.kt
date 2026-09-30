package com.cadeteria.cadete.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cadeteria.cadete.R

/** El naranja del anillo del logo (el de la imagen, un poco más vivo que CademOrange). */
private val NaranjaAnillo = Color(0xFFFB6D01)

/**
 * Logo de Cadem (2026-09-29): el centro es la imagen ("CADEM cadeteria") y el anillo naranja y
 * blanco se dibuja aparte, para poder girarlo solo a él. Con [animado], el anillo gira lento y el
 * logo entero "respira" (se agranda y achica suave) — es lo que se ve mientras se ingresa.
 * Las medidas salen del logo original: anillo entre el 95 % y el 100 % del radio, mitad naranja.
 */
@Composable
fun LogoCadem(modifier: Modifier = Modifier, tamano: Dp = 120.dp, animado: Boolean = false) {
    val transicion = rememberInfiniteTransition(label = "logo")
    val giro by transicion.animateFloat(
        0f, 360f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "giro",
    )
    val respiro by transicion.animateFloat(
        0.94f, 1.04f, infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "respiro",
    )
    Box(
        modifier
            .size(tamano)
            .graphicsLayer {
                val escala = if (animado) respiro else 1f
                scaleX = escala
                scaleY = escala
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.logo_cadem_centro),
            contentDescription = "Cadem cadetería",
            modifier = Modifier.size(tamano * 0.955f),
        )
        Canvas(Modifier.fillMaxSize()) {
            val radio = size.minDimension / 2
            val grosor = radio * 0.047f
            val radioAnillo = radio - grosor / 2
            val esquina = Offset(center.x - radioAnillo, center.y - radioAnillo)
            val caja = Size(radioAnillo * 2, radioAnillo * 2)
            rotate(if (animado) giro else 0f) {
                // En el original: naranja desde abajo a la izquierda hasta arriba a la derecha, blanco el resto.
                drawArc(NaranjaAnillo, 122f, 176f, useCenter = false, topLeft = esquina, size = caja, style = Stroke(grosor))
                drawArc(Color.White, 298f, 184f, useCenter = false, topLeft = esquina, size = caja, style = Stroke(grosor))
            }
        }
    }
}

/**
 * Una calle con una moto y una bici que la cruzan de izquierda a derecha, en loop (2026-09-29): va
 * debajo del logo mientras se ingresa. La moto es más rápida y sale antes que la bici.
 */
@Composable
fun CalleConVehiculos(modifier: Modifier = Modifier, ancho: Dp = 220.dp) {
    val transicion = rememberInfiniteTransition(label = "calle")
    val moto by transicion.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1_800, easing = LinearEasing)), label = "moto",
    )
    val bici by transicion.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2_600, easing = LinearEasing), initialStartOffset = StartOffset(700)), label = "bici",
    )
    val icono = 30.dp
    BoxWithConstraints(
        modifier
            .width(ancho)
            .height(icono + 10.dp)
            .clipToBounds(),
        contentAlignment = Alignment.BottomStart,
    ) {
        val recorrido = maxWidth + icono
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            drawLine(
                Color.White.copy(alpha = 0.35f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                strokeWidth = size.height, cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)),
            )
        }
        Icon(
            Icons.Filled.TwoWheeler, contentDescription = null, tint = NaranjaAnillo,
            modifier = Modifier.size(icono).offset(x = recorrido * moto - icono, y = (-4).dp),
        )
        Icon(
            Icons.Filled.PedalBike, contentDescription = null, tint = Color.White,
            modifier = Modifier.size(icono).offset(x = recorrido * bici - icono, y = (-4).dp),
        )
    }
}
