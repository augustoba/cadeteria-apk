package com.cadeteria.cadete.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalleDelTelefonoTest {

    @Test
    fun `la altura sale del primer numero`() {
        assertEquals(690, alturaDe("690"))
        assertEquals(690, alturaDe("690-700"))
        assertNull(alturaDe("s/n"))
        assertNull(alturaDe(null))
    }

    @Test
    fun `resuelve la primera vez, despues solo si se movio 120 m o pasaron 2 minutos`() {
        val t = ThrottleCalle()
        val lat = -26.83
        val lng = -65.20
        assertTrue(t.debeResolver(lat, lng, 10f, 0))
        t.registrar(lat, lng, 0)
        assertFalse("quieto y enseguida", t.debeResolver(lat, lng, 10f, 30_000))
        assertTrue("se movió ~150 m", t.debeResolver(lat + 0.00135, lng, 10f, 30_000))
        assertTrue("pasaron 2 minutos", t.debeResolver(lat, lng, 10f, 120_000))
    }

    @Test
    fun `con mala precision o sin precision no resuelve`() {
        val t = ThrottleCalle()
        assertFalse(t.debeResolver(-26.83, -65.2, 80f, 0))
        assertFalse(t.debeResolver(-26.83, -65.2, null, 0))
    }
}
