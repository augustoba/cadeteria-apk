package com.cadeteria.cadete.data.local

import android.content.SharedPreferences
import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.ui.home.AvisosCalleTexto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Avisos de la calle activos que conoce la app (2026-09-29): los que llegan en vivo, los que se piden
 * al abrir o al pasar a Libre, y sus cambios ("sigue" extiende, "ya no está" los baja). Vive en
 * CadeteApp para que lo usen Inicio (la lista) y el servicio de ubicación ("¿Sigue ahí?").
 *
 * Los propios y los ya preguntados quedan guardados en [prefs] (bug 2026-09-29): en memoria se
 * perdían cada vez que la app o el servicio de ubicación se reiniciaban, y el cadete volvía a recibir
 * "¿Sigue ahí?" por el mismo aviso — hasta por el suyo, cuyo voto el backend rechaza.
 */
class AvisosCalleStore(private val prefs: SharedPreferences? = null) {

    private companion object {
        const val CLAVE_MIOS = "avisos_calle_mios"
        const val CLAVE_PREGUNTADOS = "avisos_calle_preguntados"
        /** Los avisos duran horas: alcanza con recordar los últimos, así la lista no crece para siempre. */
        const val MAX_GUARDADOS = 200
    }

    private val _avisos = MutableStateFlow<List<AvisoCalleDto>>(emptyList())
    val avisos: StateFlow<List<AvisoCalleDto>> = _avisos.asStateFlow()

    /** Los que avisó este cadete (no se le pregunta "¿Sigue ahí?" por los suyos). */
    private val _mios = cargar(CLAVE_MIOS)
    val mios: Set<String> get() = synchronized(this) { _mios.toSet() }

    /** Por los que ya se le preguntó "¿Sigue ahí?" (una sola vez por aviso). */
    private val _preguntados = cargar(CLAVE_PREGUNTADOS)
    val preguntados: Set<String> get() = synchronized(this) { _preguntados.toSet() }

    /** Agrega o actualiza uno. Devuelve true si es nuevo (para notificar solo la primera vez). */
    fun recibir(aviso: AvisoCalleDto): Boolean {
        // El backend dice cuáles son propios: así no depende de lo que se recuerde en el teléfono.
        if (aviso.mio == true && aviso.id !in _mios) agregarGuardado(_mios, CLAVE_MIOS, aviso.id)
        val nuevo = _avisos.value.none { it.id == aviso.id }
        _avisos.value = (listOf(aviso) + _avisos.value.filter { it.id != aviso.id }).filter { AvisosCalleTexto.vigente(it) }
        return nuevo && AvisosCalleTexto.vigente(aviso)
    }

    fun reemplazar(lista: List<AvisoCalleDto>) {
        lista.filter { it.mio == true && it.id !in _mios }.forEach { agregarGuardado(_mios, CLAVE_MIOS, it.id) }
        _avisos.value = lista.filter { AvisosCalleTexto.vigente(it) }
    }

    fun marcarMio(aviso: AvisoCalleDto) {
        agregarGuardado(_mios, CLAVE_MIOS, aviso.id)
        recibir(aviso)
    }

    fun marcarPreguntado(avisoId: String) = agregarGuardado(_preguntados, CLAVE_PREGUNTADOS, avisoId)

    fun vigentes(): List<AvisoCalleDto> = _avisos.value.filter { AvisosCalleTexto.vigente(it) }

    private fun cargar(clave: String): LinkedHashSet<String> =
        LinkedHashSet(prefs?.getString(clave, null)?.split(',')?.filter { it.isNotBlank() } ?: emptyList())

    private fun agregarGuardado(conjunto: LinkedHashSet<String>, clave: String, id: String) = synchronized(this) {
        conjunto.remove(id)
        conjunto.add(id)
        while (conjunto.size > MAX_GUARDADOS) conjunto.remove(conjunto.first())
        prefs?.edit()?.putString(clave, conjunto.joinToString(","))?.apply()
    }
}
