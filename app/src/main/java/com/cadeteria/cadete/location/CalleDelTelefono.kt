package com.cadeteria.cadete.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Calle, altura y localidad de un punto según el Geocoder del teléfono. */
data class CalleDetectada(val calle: String, val altura: Int?, val localidad: String?)

/**
 * Calle y altura de un punto con el Geocoder de Android (2026-09-26): usa los datos de Google del
 * teléfono, gratis y sin key, que en Tucumán tienen alturas que a OpenStreetMap le faltan. El
 * backend lo guarda como `android_geocoder` y lo hace vencer igual que lo de Google.
 * Null si el teléfono no tiene Geocoder, no hay conexión, tarda más de [esperaMs] o no encontró calle.
 */
suspend fun calleDelTelefono(context: Context, lat: Double, lng: Double, esperaMs: Long = 3_000): CalleDetectada? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale("es", "AR"))
    val direccion: Address? = withTimeoutOrNull(esperaMs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }
        } else {
            // Antes de Android 13 solo existe la versión bloqueante: se corre fuera del hilo principal.
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
            }
        }
    }
    return direccion?.aCalleDetectada()
}

/** "690" o "690-700" → 690; sin número → null. */
internal fun alturaDe(subThoroughfare: String?): Int? =
    subThoroughfare?.let { Regex("\\d{1,5}").find(it)?.value?.toIntOrNull() }?.takeIf { it > 0 }

private fun Address.aCalleDetectada(): CalleDetectada? {
    val calle = thoroughfare?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return CalleDetectada(calle, alturaDe(subThoroughfare), locality ?: subAdminArea)
}

/**
 * Cuándo vale la pena resolver la calle mientras el cadete anda: no en cada ping (batería y límite
 * del Geocoder), sino cuando se movió ~120 m desde la última o pasaron 2 minutos, y solo con buena
 * precisión (con un punto impreciso el Geocoder devuelve la cuadra de al lado). Lógica pura, testeable.
 */
class ThrottleCalle(
    private val distanciaMinM: Double = 120.0,
    private val intervaloMaxMs: Long = 120_000,
    private val precisionMaxM: Float = 30f,
) {
    private var ultimaLat: Double? = null
    private var ultimaLng: Double? = null
    private var ultimaEn = 0L

    fun debeResolver(lat: Double, lng: Double, precisionM: Float?, ahoraMs: Long): Boolean {
        if (precisionM == null || precisionM > precisionMaxM) return false
        val lat0 = ultimaLat ?: return true
        val lng0 = ultimaLng ?: return true
        return AvisoLlegada.distanciaM(lat0, lng0, lat, lng) >= distanciaMinM || ahoraMs - ultimaEn >= intervaloMaxMs
    }

    fun registrar(lat: Double, lng: Double, ahoraMs: Long) {
        ultimaLat = lat
        ultimaLng = lng
        ultimaEn = ahoraMs
    }
}
