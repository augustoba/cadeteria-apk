package com.cadeteria.cadete.ui.mapacalle

import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.ui.home.AvisosCalleTexto

/** Lógica pura del "Mapa de la calle" (2026-09-29), aparte para poder testearla. */
object MapaCalleAvisos {

    /**
     * Los que trajo el servidor (toda la ciudad) más los que llegaron en vivo (los cercanos, con
     * los cambios por "¿Sigue ahí?"). Si están en los dos gana el en vivo, que es más nuevo. Solo
     * vigentes, el más nuevo primero.
     */
    fun combinar(
        delServidor: List<AvisoCalleDto>,
        enVivo: List<AvisoCalleDto>,
        ahoraMs: Long = System.currentTimeMillis(),
    ): List<AvisoCalleDto> {
        val porId = LinkedHashMap<String, AvisoCalleDto>()
        delServidor.forEach { porId[it.id] = it }
        enVivo.forEach { porId[it.id] = it }
        return porId.values
            .filter { AvisosCalleTexto.vigente(it, ahoraMs) }
            .sortedByDescending { AvisosCalleTexto.parsearIso(it.creadoEn) ?: 0L }
    }

    /** "No hay avisos activos ahora." / "1 aviso activo" / "3 avisos activos". */
    fun resumen(cantidad: Int): String = when (cantidad) {
        0 -> "No hay avisos activos ahora."
        1 -> "1 aviso activo"
        else -> "$cantidad avisos activos"
    }
}
