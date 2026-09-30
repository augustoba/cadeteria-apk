package com.cadeteria.cadete.ui.viaje

import com.cadeteria.cadete.ui.common.formatearCuentaRegresiva
import com.cadeteria.cadete.ui.common.parsearInstanteUtc

/**
 * Minutos mínimos entre Retirado y Finalizar (2026-09-29, `minutos_minimos_retiro_entrega` en
 * Configuración). La app deshabilita "Finalizar" con la cuenta regresiva; el backend vuelve a
 * controlar con la hora del toque, así la cola sin señal también respeta la espera.
 */
object EsperaEntrega {

    const val MINUTOS_DEFAULT = 10

    /** Segundos que faltan para poder finalizar; 0 = ya puede (o el control está apagado). */
    fun segundosRestantes(retiradoEnIso: String?, ahoraMs: Long, minutosMinimos: Int): Int {
        if (minutosMinimos <= 0 || retiradoEnIso.isNullOrBlank()) return 0
        val retiradoMs = parsearInstanteUtc(retiradoEnIso) ?: return 0
        val restanteMs = retiradoMs + minutosMinimos * 60_000L - ahoraMs
        return if (restanteMs <= 0) 0 else ((restanteMs + 999) / 1000).toInt()
    }

    fun mensaje(segundosRestantes: Int, minutosMinimos: Int): String =
        "Podés finalizar en ${formatearCuentaRegresiva(segundosRestantes)} " +
            "(hay que esperar $minutosMinimos min desde el retiro)."
}
