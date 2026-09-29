package com.cadeteria.cadete.ui.mapacalle

import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MapaCalleAvisosTest {

    // 2026-09-29T15:00:00Z
    private val ahora = 1_790_694_000_000L

    private fun aviso(id: String, creadoEn: String, venceEn: String, calle: String? = "Laprida 100") =
        AvisoCalleDto(id, "CONTROL", "Control", -26.83, -65.20, calle, creadoEn, venceEn)

    @Test
    fun `junta servidor y en vivo sin repetir, gana el en vivo y saca los vencidos`() {
        val servidor = listOf(
            aviso("a", "2026-09-29T14:10:00Z", "2026-09-29T15:10:00Z", calle = "vieja"),
            aviso("b", "2026-09-29T14:30:00Z", "2026-09-29T15:30:00Z"),
            aviso("vencido", "2026-09-29T13:00:00Z", "2026-09-29T14:00:00Z"),
        )
        val enVivo = listOf(
            aviso("a", "2026-09-29T14:10:00Z", "2026-09-29T15:40:00Z", calle = "nueva"),
            aviso("c", "2026-09-29T14:50:00Z", "2026-09-29T15:50:00Z"),
            // Bajado por "ya no está": vence ya.
            aviso("b", "2026-09-29T14:30:00Z", "2026-09-29T14:59:00Z"),
        )

        val resultado = MapaCalleAvisos.combinar(servidor, enVivo, ahora)

        assertEquals(listOf("c", "a"), resultado.map { it.id })
        assertEquals("nueva", resultado.last().calle)
    }

    @Test
    fun `resumen segun cantidad`() {
        assertEquals("No hay avisos activos ahora.", MapaCalleAvisos.resumen(0))
        assertEquals("1 aviso activo", MapaCalleAvisos.resumen(1))
        assertEquals("3 avisos activos", MapaCalleAvisos.resumen(3))
    }
}
