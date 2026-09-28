package com.cadeteria.cadete.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Retirado / Entregado solo en el lugar (2026-09-28): la misma regla que el backend. */
class ControlEnLugarTest {

    private val puntoLat = -26.8300
    private val puntoLng = -65.2000

    private fun ubic(dLat: Double, precision: Float? = 10f, simulada: Boolean = false) =
        UbicacionMarcada(puntoLat + dLat, puntoLng, precision, simulada = simulada)

    @Test
    fun cercaDeja() {
        val r = ControlEnLugar.evaluar(ubic(0.0005), puntoLat, puntoLng)
        assertTrue(r is ControlEnLugar.Resultado.Ok)
        assertEquals(false, (r as ControlEnLugar.Resultado.Ok).imprecisa)
    }

    @Test
    fun lejosNoDejaYDiceLaDistancia() {
        val r = ControlEnLugar.evaluar(ubic(0.0072), puntoLat, puntoLng)
        assertTrue(r is ControlEnLugar.Resultado.Lejos)
        val metros = (r as ControlEnLugar.Resultado.Lejos).distanciaM
        assertTrue("distancia $metros", metros in 750..850)
    }

    @Test
    fun sinUbicacionYSimulada() {
        assertEquals(ControlEnLugar.Resultado.SinUbicacion, ControlEnLugar.evaluar(null, puntoLat, puntoLng))
        assertEquals(ControlEnLugar.Resultado.Simulada, ControlEnLugar.evaluar(ubic(0.0, simulada = true), puntoLat, puntoLng))
    }

    @Test
    fun gpsImprecisoSumaElErrorAlRadio() {
        // ~250 m con 200 m de error: 150 + 200 de tolerancia.
        val r = ControlEnLugar.evaluar(ubic(0.00225, precision = 200f), puntoLat, puntoLng)
        assertTrue(r is ControlEnLugar.Resultado.Ok)
        assertEquals(true, (r as ControlEnLugar.Resultado.Ok).imprecisa)
    }

    @Test
    fun radioConfigurable() {
        assertTrue(ControlEnLugar.evaluar(ubic(0.0018), puntoLat, puntoLng, radioM = 150) is ControlEnLugar.Resultado.Lejos)
        assertTrue(ControlEnLugar.evaluar(ubic(0.0018), puntoLat, puntoLng, radioM = 300) is ControlEnLugar.Resultado.Ok)
    }

    @Test
    fun textos() {
        assertEquals("800 m", ControlEnLugar.textoDistancia(800))
        assertEquals("1,2 km", ControlEnLugar.textoDistancia(1234))
        assertEquals("1970-01-01T00:00:00Z", ControlEnLugar.ahoraIso(0))
    }
}
