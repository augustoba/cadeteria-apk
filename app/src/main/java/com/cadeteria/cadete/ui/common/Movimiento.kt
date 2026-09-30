package com.cadeteria.cadete.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Vibraciones de la app (2026-09-29, pedido del usuario): el cadete toca con guantes o arriba de la moto y
 * no siempre sabe si tocó. Cada una se distingue sin mirar: toque (clic), éxito (una corta), error (dos
 * golpecitos) y fuerte (rechazar). Se apagan desde Ajustes → "Vibrar al tocar".
 */
object Vibracion {
    private const val PREFS = "ajustes_app"
    private const val CLAVE = "vibrar_al_tocar"

    fun habilitada(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLAVE, true)

    fun setHabilitada(context: Context, valor: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLAVE, valor).apply()
    }

    fun toque(context: Context) = vibrar(context, Build.VERSION_CODES.Q, VibrationEffect.EFFECT_TICK, longArrayOf(0, 12))
    fun exito(context: Context) = vibrar(context, Build.VERSION_CODES.Q, VibrationEffect.EFFECT_CLICK, longArrayOf(0, 45))
    fun error(context: Context) = vibrar(context, Build.VERSION_CODES.Q, VibrationEffect.EFFECT_DOUBLE_CLICK, longArrayOf(0, 40, 90, 40))
    fun fuerte(context: Context) = vibrar(context, Build.VERSION_CODES.Q, VibrationEffect.EFFECT_HEAVY_CLICK, longArrayOf(0, 160))

    private fun vibrar(context: Context, apiPredefinido: Int, predefinido: Int, patron: LongArray) {
        if (!habilitada(context)) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            when {
                Build.VERSION.SDK_INT >= apiPredefinido -> vibrator.vibrate(VibrationEffect.createPredefined(predefinido))
                Build.VERSION.SDK_INT >= 26 -> vibrator.vibrate(VibrationEffect.createWaveform(patron, -1))
                else -> @Suppress("DEPRECATION") vibrator.vibrate(patron, -1)
            }
        }
    }
}

/**
 * Botón que se hunde un poco al apretarlo y vuelve con rebote, con un "clic" de vibración. Se pone
 * sobre cualquier Button sin cambiar su onClick: escucha el toque sin consumirlo.
 */
fun Modifier.efectoToque(vibrar: Boolean = true): Modifier = composed {
    val context = LocalContext.current
    var apretado by remember { mutableStateOf(false) }
    val escala by animateFloatAsState(
        if (apretado) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "efectoToque",
    )
    this
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
        .pointerInput(vibrar) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                apretado = true
                if (vibrar) Vibracion.toque(context)
                waitForUpOrCancellation(PointerEventPass.Initial)
                apretado = false
            }
        }
}

/** Tiembla de lado a lado ("no") cada vez que [disparador] cambia (y no es 0). */
fun Modifier.temblor(disparador: Int): Modifier = composed {
    val desplazamiento = remember { Animatable(0f) }
    LaunchedEffect(disparador) {
        if (disparador == 0) return@LaunchedEffect
        desplazamiento.animateTo(
            0f,
            keyframes {
                durationMillis = 360
                -14f at 45
                14f at 110
                -10f at 175
                10f at 240
                -4f at 300
            },
        )
    }
    graphicsLayer { translationX = desplazamiento.value * density }
}

/** Late despacio (se agranda y achica) mientras [activo]; con [urgente] además tiembla un poquito. */
fun Modifier.latido(activo: Boolean, urgente: Boolean = false): Modifier = composed {
    if (!activo) return@composed this
    val transicion = rememberInfiniteTransition(label = "latido")
    val escala by transicion.animateFloat(
        1f, if (urgente) 1.06f else 1.04f,
        infiniteRepeatable(tween(if (urgente) 380 else 900), RepeatMode.Reverse), label = "latidoEscala",
    )
    val giro by transicion.animateFloat(
        -1.5f, 1.5f, infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse), label = "latidoGiro",
    )
    graphicsLayer {
        scaleX = escala
        scaleY = escala
        rotationZ = if (urgente) giro else 0f
    }
}

/**
 * Tilde grande que aparece con un "pop" (aceptó un viaje, lo entregó). Vibra de éxito al aparecer y
 * llama a [alTerminar] a los [duracionMs]. Con [monto], la plata del viaje se va sumando de 0 al total.
 */
@Composable
fun ConfirmacionAnimada(
    titulo: String,
    color: Color,
    alTerminar: () -> Unit,
    monto: Double? = null,
    duracionMs: Long = 1_300,
) {
    val context = LocalContext.current
    val escala = remember { Animatable(0.3f) }
    val cuenta = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        Vibracion.exito(context)
        escala.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(Unit) {
        if (monto != null) cuenta.animateTo(monto.toFloat(), tween(700))
    }
    LaunchedEffect(Unit) {
        delay(duracionMs)
        alTerminar()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown() } }, // no deja tocar lo de atrás
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            },
        ) {
            Box(Modifier.size(120.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
            }
            Text(titulo, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (monto != null) {
                Text(
                    "+ " + formatearPesos(cuenta.value.toDouble()),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
