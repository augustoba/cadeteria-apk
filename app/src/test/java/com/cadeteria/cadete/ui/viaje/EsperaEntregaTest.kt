package com.cadeteria.cadete.ui.viaje

import org.junit.Assert.assertEquals
import org.junit.Test

/** Minutos mínimos entre Retirado y Finalizar (2026-09-29): la misma regla que el backend. */
class EsperaEntregaTest {

    private val retirado = "2026-09-29T20:00:00Z"
    private val retiradoMs = 1_790_712_000_000L // 2026-09-29T20:00:00Z

    @Test
    fun recienRetiradoFaltanLosDiezMinutos() {
        assertEquals(600, EsperaEntrega.segundosRestantes(retirado, retiradoMs, 10))
    }

    @Test
    fun aLosSieteMinutosFaltanTres() {
        assertEquals(180, EsperaEntrega.segundosRestantes(retirado, retiradoMs + 7 * 60_000, 10))
    }

    @Test
    fun pasadosLosDiezDeja() {
        assertEquals(0, EsperaEntrega.segundosRestantes(retirado, retiradoMs + 10 * 60_000, 10))
        assertEquals(0, EsperaEntrega.segundosRestantes(retirado, retiradoMs + 60 * 60_000, 10))
    }

    @Test
    fun enCeroEstaApagado() {
        assertEquals(0, EsperaEntrega.segundosRestantes(retirado, retiradoMs, 0))
    }

    @Test
    fun sinHoraDeRetiroNoFrena() {
        // Sin Retirado ya lo frena "Primero marcá Retirado"; una hora ilegible no debe trabar al cadete.
        assertEquals(0, EsperaEntrega.segundosRestantes(null, retiradoMs, 10))
        assertEquals(0, EsperaEntrega.segundosRestantes("basura", retiradoMs, 10))
    }

    @Test
    fun conMilisegundosYZonaTambienParsea() {
        assertEquals(600, EsperaEntrega.segundosRestantes("2026-09-29T20:00:00.123Z", retiradoMs, 10))
    }
}
