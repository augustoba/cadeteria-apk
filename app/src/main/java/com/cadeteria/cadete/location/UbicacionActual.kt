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
)

/**
 * Lectura de GPS nueva y precisa para Retirado/Entregado (2026-09-26): con buena precisión el
 * backend aprende las coordenadas de esa dirección (la puerta real). Espera hasta [esperaMs] a que
 * el GPS fije; si no llega, usa la última conocida (puede ser vieja: su precisión lo dice).
 */
@SuppressLint("MissingPermission")
suspend fun ubicacionPrecisa(context: Context, esperaMs: Long = 8_000): UbicacionMarcada? {
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
        val calle = if (precision != null && precision <= 50f) calleDelTelefono(context, fresca.latitude, fresca.longitude) else null
        return UbicacionMarcada(fresca.latitude, fresca.longitude, precision, calle)
    }
    return ubicacionActual(context)?.let { UbicacionMarcada(it.first, it.second, null) }
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
