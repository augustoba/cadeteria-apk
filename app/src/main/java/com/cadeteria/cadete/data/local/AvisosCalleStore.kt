package com.cadeteria.cadete.data.local

import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.ui.home.AvisosCalleTexto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Avisos de la calle activos que conoce la app (2026-09-29): los que llegan en vivo, los que se piden
 * al abrir o al pasar a Libre, y sus cambios ("sigue" extiende, "ya no está" los baja). Vive en
 * CadeteApp para que lo usen Inicio (la lista) y el servicio de ubicación ("¿Sigue ahí?").
 */
class AvisosCalleStore {

    private val _avisos = MutableStateFlow<List<AvisoCalleDto>>(emptyList())
    val avisos: StateFlow<List<AvisoCalleDto>> = _avisos.asStateFlow()

    /** Los que avisó este cadete (no se le pregunta "¿Sigue ahí?" por los suyos). */
    private val _mios = mutableSetOf<String>()
    val mios: Set<String> get() = _mios.toSet()

    /** Agrega o actualiza uno. Devuelve true si es nuevo (para notificar solo la primera vez). */
    fun recibir(aviso: AvisoCalleDto): Boolean {
        val nuevo = _avisos.value.none { it.id == aviso.id }
        _avisos.value = (listOf(aviso) + _avisos.value.filter { it.id != aviso.id }).filter { AvisosCalleTexto.vigente(it) }
        return nuevo && AvisosCalleTexto.vigente(aviso)
    }

    fun reemplazar(lista: List<AvisoCalleDto>) {
        _avisos.value = lista.filter { AvisosCalleTexto.vigente(it) }
    }

    fun marcarMio(aviso: AvisoCalleDto) {
        _mios += aviso.id
        recibir(aviso)
    }

    fun vigentes(): List<AvisoCalleDto> = _avisos.value.filter { AvisosCalleTexto.vigente(it) }
}
