package com.cadeteria.cadete.location

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Retirado / parada / Entregado solo en el lugar (2026-09-28). Lógica pura (sin Android) para
 * poder testearla. Es la misma regla que aplica el backend (PedidoService.controlarEnLugar), que
 * vuelve a controlar con la posición que le llega:
 * - GPS falso → no deja (y se le avisa al backend para que quede registrado).
 * - Sin ubicación → no deja, salvo "Estoy en el lugar" con foto.
 * - Más lejos que el radio → no deja, salvo "Estoy en el lugar" con foto.
 * - GPS impreciso (error mayor al tope, típico adentro de un local) → suma el error al radio y deja.
 */
object ControlEnLugar {

    const val RADIO_M_DEFAULT = 150
    const val PRECISION_MAX_M_DEFAULT = 100

    sealed class Resultado {
        /** Puede marcar. imprecisa = el GPS tenía mucho error (queda anotado en el pedido). */
        data class Ok(val distanciaM: Int, val imprecisa: Boolean) : Resultado()
        data class Lejos(val distanciaM: Int) : Resultado()
        data object SinUbicacion : Resultado()
        data object Simulada : Resultado()
    }

    fun evaluar(
        ubicacion: UbicacionMarcada?,
        puntoLat: Double,
        puntoLng: Double,
        radioM: Int = RADIO_M_DEFAULT,
        precisionMaxM: Int = PRECISION_MAX_M_DEFAULT,
    ): Resultado {
        if (ubicacion == null) return Resultado.SinUbicacion
        if (ubicacion.simulada) return Resultado.Simulada
        val distancia = AvisoLlegada.distanciaM(ubicacion.lat, ubicacion.lng, puntoLat, puntoLng).roundToInt()
        val precision = ubicacion.precisionM
        val imprecisa = precision != null && precision > precisionMaxM
        val tolerancia = radioM + if (imprecisa) precision!!.roundToInt() else 0
        return if (distancia <= tolerancia) Resultado.Ok(distancia, imprecisa) else Resultado.Lejos(distancia)
    }

    /** "800 m" / "1,2 km", como lo dice el backend. */
    fun textoDistancia(metros: Int): String =
        if (metros < 1000) "$metros m" else String.format(Locale.forLanguageTag("es-AR"), "%.1f km", metros / 1000.0)

    /** Hora del toque en ISO UTC ("2026-09-28T17:32:05Z"): sin java.time para andar en Android 7. */
    fun ahoraIso(ahoraMs: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(ahoraMs))
}
