package com.cadeteria.cadete.location

import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.ui.home.AvisosCalleTexto

/**
 * "¿Sigue ahí?" (avisos de la calle, segunda etapa, 2026-09-29). Lógica pura (sin Android) para poder
 * testearla: con cada ping de ubicación, decide si hay que preguntarle al cadete por un aviso activo
 * que tiene a menos de [radioM]. Una sola vez por aviso, nunca por los propios, y solo con una
 * ubicación que sirva (error de hasta [precisionMaximaM]).
 */
class PreguntaSigueAhi(
    private val radioM: Double = RADIO_M,
    private val precisionMaximaM: Float = PRECISION_MAXIMA_M,
    /** Los ya preguntados antes de un reinicio (guardados en el teléfono, bug 2026-09-29). */
    preguntadosIniciales: Set<String> = emptySet(),
    /** Para guardar cada aviso preguntado y que sobreviva al reinicio de la app o del servicio. */
    private val alPreguntar: (String) -> Unit = {},
) {
    companion object {
        const val RADIO_M = 100.0
        const val PRECISION_MAXIMA_M = 100f
    }

    private val preguntados = preguntadosIniciales.toMutableSet()

    /** El aviso por el que hay que preguntar ahora (el más cercano), o null. Lo marca como preguntado. */
    fun revisar(
        avisos: List<AvisoCalleDto>,
        misAvisos: Set<String>,
        lat: Double,
        lng: Double,
        precisionM: Float?,
        ahoraMs: Long = System.currentTimeMillis(),
    ): AvisoCalleDto? {
        if (precisionM != null && precisionM > precisionMaximaM) return null
        val aviso = avisos
            .asSequence()
            .filter { it.id !in preguntados && it.id !in misAvisos && AvisosCalleTexto.vigente(it, ahoraMs) }
            .map { it to AvisoLlegada.distanciaM(lat, lng, it.lat, it.lng) }
            .filter { (_, d) -> d <= radioM }
            .minByOrNull { (_, d) -> d }
            ?.first ?: return null
        preguntados += aviso.id
        alPreguntar(aviso.id)
        return aviso
    }
}
