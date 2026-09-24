package com.cadeteria.cadete.ui.viaje

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.ContadorAceptacion
import com.cadeteria.cadete.ui.common.RutaRetiroEntrega
import com.cadeteria.cadete.ui.theme.Emerald600
import com.cadeteria.cadete.ui.theme.EstiloNumero
import com.cadeteria.cadete.ui.theme.Gray500
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Oferta de viaje como pantalla completa propia (spec-app-mejoras-visuales §3): sin mapa ni
 * barra superior, el cadete ve la oferta y decide. Sigue el tema del sistema (decisión 1): los
 * colores salen de MaterialTheme; solo el anillo mantiene su acento (naranja, rojo al final).
 *
 * Muestra lo mismo que se veía antes de aceptar — el detalle, el piso/depto y las observaciones
 * aparecen recién al aceptar (el backend los manda en null hasta entonces).
 */
@Composable
fun OfertaPantalla(
    viaje: PedidoDto,
    tiempoLimiteSeg: Int,
    enviando: Boolean,
    error: String?,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Viaje nuevo", style = MaterialTheme.typography.titleLarge)
            Text("Pedido #${viaje.numero}", style = MaterialTheme.typography.bodyMedium, color = Gray500)
            Spacer(Modifier.height(20.dp))

            ContadorAceptacion(viaje.asignadoEn, tiempoLimiteSeg, grande = true)
            Spacer(Modifier.height(20.dp))

            error?.let {
                BannerError(it, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DatoOferta(
                    "Distancia",
                    "≈ " + formatearKm(distanciaKm(viaje.origenLat, viaje.origenLng, viaje.destinoLat, viaje.destinoLng)),
                    "en línea recta",
                    Modifier.weight(1f),
                )
                DatoOferta("Pago", formatearPesos(viaje.precio), null, Modifier.weight(1f))
                val dinero = viaje.montoDeclarado ?: 0.0
                DatoOferta(
                    "Dinero",
                    if (dinero > 0) formatearPesos(dinero) else "No",
                    if (viaje.llevaValores) "y lleva valores" else null,
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    RutaRetiroEntrega(viaje.origenDireccion, viaje.destinoDireccion)
                    if (viaje.requiereMoto) {
                        Spacer(Modifier.height(10.dp))
                        Text("🏍️ Requiere moto", style = MaterialTheme.typography.labelLarge, color = Gray500)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            if (enviando) {
                CircularProgressIndicator()
            } else {
                // Aceptar más grande que Rechazar (~70/30, spec §3 cambio 4): la acción esperada
                // es la que queda bajo el pulgar.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onAceptar,
                        modifier = Modifier.weight(0.7f).height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    ) { Text("✅ Aceptar", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    OutlinedButton(
                        onClick = onRechazar,
                        modifier = Modifier.weight(0.3f).height(60.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    ) { Text("Rechazar") }
                }
            }
            Spacer(Modifier.height(16.dp))
            // Las reglas, a la vista antes de decidir (pendientes.md, punto 1).
            Text(
                "Si no respondés a tiempo, se le ofrece a otro cadete (y más tarde te lo pueden volver a " +
                    "ofrecer). Rechazar tiene un límite por viaje.",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DatoOferta(etiqueta: String, valor: String, nota: String?, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Gray500)
            Text(valor, style = EstiloNumero.copy(fontSize = 19.sp), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            nota?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Gray500) }
        }
    }
}

private fun formatearPesos(monto: Double): String =
    "$" + NumberFormat.getIntegerInstance(Locale("es", "AR")).format(monto.toLong())

private fun formatearKm(km: Double): String = String.format(Locale("es", "AR"), "%.1f km", km)

/** Haversine — la ruta que pide la app va del cadete al destino, no sirve para la distancia del viaje. */
private fun distanciaKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
