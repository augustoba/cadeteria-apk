package com.cadeteria.cadete.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Posición al marcar Retirado/Entregado, con su error en metros (null si el teléfono no lo informa). */
data class UbicacionMarcada(
    val lat: Double,
    val lng: Double,
    val precisionM: Float?,
    /** Calle según el Geocoder del teléfono (solo con buena precisión) — ayuda a confirmar la dirección. */
    val calle: CalleDetectada? = null,
    /** La dio una app de ubicación simulada (GPS falso) — 2026-09-28: con esto no se deja marcar. */
    val simulada: Boolean = false,
)

/** Última conocida que todavía sirve para decir "está en el lugar" (más vieja, el cadete pudo haberse movido). */
private const val ULTIMA_CONOCIDA_MAX_MS = 2 * 60_000L

/** Si la ubicación la inventó una app de GPS falso (API 31+ isMock, antes isFromMockProvider). */
@Suppress("DEPRECATION")
private fun android.location.Location.esSimulada(): Boolean =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) isMock else isFromMockProvider

/**
 * Lectura de GPS nueva y precisa para Retirado/Entregado (2026-09-26): con buena precisión el
 * backend aprende las coordenadas de esa dirección (la puerta real). Espera hasta [esperaMs] a que
 * el GPS fije — sin datos móviles el GPS anda igual pero tarda más en ubicarse (2026-09-28: la
 * pantalla muestra "Buscando tu ubicación…"). Si no llega, usa la última conocida solo si es de
 * hace menos de 2 minutos; si no, null (el cadete puede usar "Estoy en el lugar").
 */
@SuppressLint("MissingPermission")
suspend fun ubicacionPrecisa(context: Context, esperaMs: Long = 30_000): UbicacionMarcada? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val fresca = withTimeoutOrNull(esperaMs) {
        suspendCancellableCoroutine<android.location.Location?> { cont ->
            val token = CancellationTokenSource()
            cont.invokeOnCancellation { token.cancel() }
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }
    if (fresca != null) {
        val precision = if (fresca.hasAccuracy()) fresca.accuracy else null
        val simulada = fresca.esSimulada()
        val calle = if (!simulada && precision != null && precision <= 50f) {
            calleDelTelefono(context, fresca.latitude, fresca.longitude)
        } else null
        return UbicacionMarcada(fresca.latitude, fresca.longitude, precision, calle, simulada)
    }
    val ultima = ultimaConocida(context) ?: return null
    if (System.currentTimeMillis() - ultima.time > ULTIMA_CONOCIDA_MAX_MS) return null
    return UbicacionMarcada(
        ultima.latitude, ultima.longitude, if (ultima.hasAccuracy()) ultima.accuracy else null,
        simulada = ultima.esSimulada(),
    )
}

@SuppressLint("MissingPermission")
private suspend fun ultimaConocida(context: Context): android.location.Location? = suspendCancellableCoroutine { cont ->
    try {
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    } catch (e: SecurityException) {
        if (cont.isActive) cont.resume(null)
    }
}

/**
 * Última ubicación conocida del celular, en un solo tiro — para adjuntar coordenadas a
 * los botones "Retirado"/"Finalizado" y que el admin pueda verificar en el mapa que
 * coinciden con la dirección declarada por el cliente (spec: "capturar la ubicacion...
 * para verificar que coincide con la que deberia ser de retiro y de finalizacion").
 * Devuelve null si no hay ubicación disponible o falta el permiso — los botones siguen
 * funcionando igual, las coordenadas son un dato opcional.
 */
@SuppressLint("MissingPermission")
suspend fun ubicacionActual(context: Context): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
    try {
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { loc ->
                if (cont.isActive) cont.resume(loc?.let { it.latitude to it.longitude })
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    } catch (e: SecurityException) {
        if (cont.isActive) cont.resume(null)
    }
}
