package com.cadeteria.cadete.push

import android.content.Context
import com.cadeteria.cadete.data.remote.dto.EstadoCadete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Recordatorio "no podés recibir pedidos" (spec-app-mejoras-visuales §5): cada 30 minutos
 * seguidos en DESCONECTADO u OCUPADO, un aviso de que en ese estado no le llegan viajes.
 *
 * - Cualquier cambio de estado reinicia la cuenta (collectLatest cancela la espera anterior).
 * - Se corta al tocar "Salir" ([detener]) — decisión 2026-09-20.
 * - Límite aceptado: vive mientras viva el proceso. En DESCONECTADO no hay servicio en primer
 *   plano, así que si Android mata la app minimizada el recordatorio se corta solo
 *   (best-effort, igual que cerrarla desde Recientes). Un service propio mostraría una
 *   notificación permanente, contradictoria con un recordatorio.
 */
class RecordatorioEstado(private val context: Context, scope: CoroutineScope) {

    /** null = sin sesión (o se tocó "Salir"): no se recuerda nada. */
    private val estado = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            estado.collectLatest { actual ->
                if (actual != EstadoCadete.DESCONECTADO && actual != EstadoCadete.OCUPADO) return@collectLatest
                while (true) {
                    delay(INTERVALO_MS)
                    NotificationHelper.mostrar(
                        context,
                        NotificationHelper.CANAL_RECORDATORIOS,
                        NOTIFICACION_ID,
                        if (actual == EstadoCadete.OCUPADO) "Estás OCUPADO" else "Estás DESCONECTADO",
                        "En este estado no te llegan viajes nuevos. Si ya podés, ponete libre.",
                    )
                }
            }
        }
    }

    /** Lo llama Inicio cada vez que conoce el estado real del cadete (al cargar y al cambiarlo). */
    fun actualizar(estadoId: String) {
        estado.value = estadoId
    }

    /** "Salir" o sesión invalidada: deja de recordar. */
    fun detener() {
        estado.value = null
    }

    private companion object {
        const val INTERVALO_MS = 30 * 60 * 1000L
        const val NOTIFICACION_ID = 7301
    }
}
