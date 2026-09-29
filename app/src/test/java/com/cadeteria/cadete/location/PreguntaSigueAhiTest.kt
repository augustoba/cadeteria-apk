package com.cadeteria.cadete.location

import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.ui.home.AvisosCalleTexto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** "¿Sigue ahí?" (segunda etapa, 2026-09-29): cuándo se le pregunta al cadete. */
class PreguntaSigueAhiTest {

    private val lat = -26.8300
    private val lng = -65.2000
    private val ahora = AvisosCalleTexto.parsearIso("2026-09-29T15:00:00Z")!!

    /** dLat 0,0005° ≈ 55 m; 0,0018° ≈ 200 m. */
    private fun aviso(id: String, dLat: Double, vence: String = "2026-09-29T15:30:00Z") =
        AvisoCalleDto(id, "CONTROL", "Control", lat + dLat, lng, "Mate de Luna 2400", "2026-09-29T14:40:00Z", vence)

    @Test
    fun preguntaPorElMasCercanoAMenosDe100mYUnaSolaVez() {
        val p = PreguntaSigueAhi()
        val avisos = listOf(aviso("lejos", 0.0018), aviso("cerca", 0.0005), aviso("mas-cerca", 0.0002))
        assertEquals("mas-cerca", p.revisar(avisos, emptySet(), lat, lng, 10f, ahora)?.id)
        assertEquals("cerca", p.revisar(avisos, emptySet(), lat, lng, 10f, ahora)?.id)
        assertNull("el de 200 m no, y los otros ya se preguntaron", p.revisar(avisos, emptySet(), lat, lng, 10f, ahora))
    }

    @Test
    fun noPreguntaPorLosPropiosNiVencidosNiConGpsMalo() {
        val p = PreguntaSigueAhi()
        assertNull(p.revisar(listOf(aviso("mio", 0.0002)), setOf("mio"), lat, lng, 10f, ahora))
        assertNull(p.revisar(listOf(aviso("vencido", 0.0002, vence = "2026-09-29T14:59:00Z")), emptySet(), lat, lng, 10f, ahora))
        assertNull(p.revisar(listOf(aviso("impreciso", 0.0002)), emptySet(), lat, lng, 300f, ahora))
        // Con GPS malo no lo marca como preguntado: se pregunta cuando mejora.
        assertEquals("impreciso", p.revisar(listOf(aviso("impreciso", 0.0002)), emptySet(), lat, lng, 20f, ahora)?.id)
    }
}
