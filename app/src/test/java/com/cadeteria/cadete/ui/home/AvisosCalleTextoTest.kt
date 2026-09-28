package com.cadeteria.cadete.ui.home

import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Avisos de la calle" (carril C, 2026-09-28): una línea corta, "hace X min" y vencimiento. */
class AvisosCalleTextoTest {

    private val creado = "2026-09-28T23:20:00.123456789Z"
    private val creadoMs = AvisosCalleTexto.parsearIso(creado)!!

    private fun aviso(calle: String?) = AvisoCalleDto(
        "a1", "CONTROL", "Control", -26.83, -65.2, calle, creado, "2026-09-29T00:20:00Z",
    )

    @Test
    fun lineaCorta() {
        assertEquals("🚓 Control · Mate de Luna 2400 · hace 1 min", AvisosCalleTexto.linea(aviso("Mate de Luna 2400"), creadoMs + 70_000))
        assertEquals("🚓 Control · cerca de tu ubicación · recién", AvisosCalleTexto.linea(aviso(null), creadoMs + 10_000))
    }

    @Test
    fun confirmacion() {
        assertEquals("Avisaste: Control en Mate de Luna 2400", AvisosCalleTexto.confirmacion(aviso("Mate de Luna 2400")))
    }

    @Test
    fun venceALaHora() {
        assertTrue(AvisosCalleTexto.vigente(aviso(null), creadoMs + 59 * 60_000))
        assertFalse(AvisosCalleTexto.vigente(aviso(null), creadoMs + 61 * 60_000))
    }

    @Test
    fun haceHoras() {
        assertEquals("hace 2 h", AvisosCalleTexto.haceCuanto(creado, creadoMs + 125 * 60_000))
    }
}
