package com.cadeteria.cadete.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadeteria.cadete.ui.theme.CademOrange
import com.cadeteria.cadete.ui.theme.Red600
import kotlinx.coroutines.delay

@Composable
fun CargandoFullScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun BannerError(mensaje: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(mensaje, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Aviso general del admin, flotando arriba de la pantalla — a diferencia de la
 * notificación del sistema (que desaparece sola y no deja rastro), esto se queda a la
 * vista hasta que el cadete lo cierra o pasan unos segundos. Se muestra encima del
 * contenido (usar dentro de un Box con Alignment.TopCenter), no en el flujo normal.
 */
@Composable
fun AvisoFlotante(mensaje: String, onCerrar: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(mensaje) {
        delay(8_000)
        onCerrar()
    }
    Card(
        modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Campaign, contentDescription = null, tint = CademOrange)
            Text(
                mensaje,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            IconButton(onClick = onCerrar) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
            }
        }
    }
}

@Composable
fun BannerInfo(mensaje: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(mensaje, color = Color(0xFF1E40AF), style = MaterialTheme.typography.bodyMedium)
    }
}

/** "2026-09-13T02:14:37Z" -> millis desde epoch (UTC), sin depender de java.time (minSdk 24 sin desugaring). */
fun parsearInstanteUtc(iso: String): Long? = runCatching {
    val formato = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
    formato.timeZone = java.util.TimeZone.getTimeZone("UTC")
    formato.parse(iso.take(19))?.time
}.getOrNull()

/** 80 -> "1:20"; negativo se trata como 0. */
fun formatearCuentaRegresiva(segundos: Int): String {
    val s = segundos.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** Umbrales (segundos restantes) en los que se dispara una vibración corta de alerta, uno por vez. */
private val UMBRALES_VIBRACION_SEG = setOf(30, 15, 5)

private fun vibrarAlerta(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(250)
    }
}

/**
 * Cuenta regresiva para aceptar un viaje asignado (PENDIENTE) — se usa tanto en el detalle
 * del viaje como en la tarjeta de "Asignados y en curso" del Home, para que el cadete vea el
 * mismo tiempo restante esté donde esté (antes solo aparecía al abrir el detalle).
 *
 * Es un anillo de progreso con el tiempo adentro (en vez de solo texto) para que se lea de un
 * vistazo en la calle, y vibra corto a los 30/15/5 segundos — un cadete con el casco puesto o
 * mirando la calle no siempre está mirando fijo la pantalla (auditoría UX 2026-09-15).
 */
@Composable
fun ContadorAceptacion(asignadoEn: String?, tiempoLimiteSeg: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val inicioMs = remember(asignadoEn) { asignadoEn?.let(::parsearInstanteUtc) }
    var segundosRestantes by remember(asignadoEn) { mutableStateOf<Int?>(null) }
    LaunchedEffect(inicioMs, tiempoLimiteSeg) {
        if (inicioMs == null) return@LaunchedEffect
        val umbralesYaVibrados = mutableSetOf<Int>()
        while (true) {
            val transcurridoSeg = (System.currentTimeMillis() - inicioMs) / 1000
            val restante = (tiempoLimiteSeg - transcurridoSeg).toInt()
            segundosRestantes = restante
            if (restante in UMBRALES_VIBRACION_SEG && umbralesYaVibrados.add(restante)) {
                runCatching { vibrarAlerta(context) }
            }
            if (restante <= 0) break
            delay(1000)
        }
    }
    segundosRestantes?.let { restante ->
        val urgente = restante <= 15
        val colorAcento = if (urgente) Red600 else MaterialTheme.colorScheme.primary
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (tiempoLimiteSeg > 0) (restante.toFloat() / tiempoLimiteSeg).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.size(56.dp),
                    color = colorAcento,
                    trackColor = colorAcento.copy(alpha = 0.15f),
                    strokeWidth = 5.dp,
                )
                Text(
                    formatearCuentaRegresiva(restante),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colorAcento,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (restante > 0) "Para responder — si se agota, se le ofrece a otro cadete" else "⏱ Se agotó el tiempo — puede que ya se le ofrezca a otro cadete",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (urgente) FontWeight.Bold else FontWeight.Normal,
                color = colorAcento,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
