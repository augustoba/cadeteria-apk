package com.cadeteria.cadete.ui.home

import com.cadeteria.cadete.data.remote.dto.AvisoGeneralDto
import com.cadeteria.cadete.data.remote.dto.CadeteConfigDto

/** Cartel "Antes de arrancar" ya resuelto: lo que se muestra. */
data class RecordatoriosCartel(val titulo: String, val textos: List<String>)

/**
 * Carteles con "Entendido" de Inicio (2026-09-29): los recordatorios al entrar y los avisos generales
 * del admin. Lógica pura para poder testearla.
 */
object CartelesInicio {

    const val TITULO_POR_DEFECTO = "Antes de arrancar"

    /** Los de siempre: valen si el backend es viejo o no se pudo leer la configuración. */
    val TEXTOS_POR_DEFECTO = listOf(
        "Llevá toda la documentación en regla (DNI, licencia, cédula del vehículo, seguro).",
        "No te olvides los elementos de seguridad: casco, cadena y mochila.",
        "Marcá cada viaje como \"Retirado\" al levantar el pedido, y \"Finalizado\" con los datos correspondientes al entregarlo.",
    )

    /** null = no sale el cartel (apagado en Configuración o sin renglones). */
    fun recordatorios(config: CadeteConfigDto?): RecordatoriosCartel? {
        if (config?.recordatoriosActivo == false) return null
        val textos = (config?.recordatoriosTextos ?: TEXTOS_POR_DEFECTO).map { it.trim() }.filter { it.isNotEmpty() }
        if (textos.isEmpty()) return null
        val titulo = config?.recordatoriosTitulo?.trim().orEmpty().ifEmpty { TITULO_POR_DEFECTO }
        return RecordatoriosCartel(titulo, textos)
    }

    /**
     * Suma avisos a la cola sin repetir (el mismo puede llegar en vivo y en "pendientes"). Los que ya
     * estaban quedan primero; los nuevos, del más viejo al más nuevo. [nuevosDelMasNuevo] viene como lo
     * manda el backend: el más nuevo primero.
     */
    fun encolar(cola: List<AvisoGeneralDto>, nuevosDelMasNuevo: List<AvisoGeneralDto>): List<AvisoGeneralDto> {
        val ids = cola.map { it.id }.toMutableSet()
        val nuevos = nuevosDelMasNuevo.reversed().filter { ids.add(it.id) }
        return cola + nuevos
    }

    /** "Aviso de la cadetería", o "Aviso (3 sin leer)" si hay más en la cola. */
    fun tituloAviso(cantidad: Int): String =
        if (cantidad <= 1) "Aviso de la cadetería" else "Aviso ($cantidad sin leer)"
}
