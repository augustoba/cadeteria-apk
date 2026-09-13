package com.cadeteria.cadete.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
