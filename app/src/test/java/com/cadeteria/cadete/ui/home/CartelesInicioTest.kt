package com.cadeteria.cadete.ui.home

import com.cadeteria.cadete.data.remote.dto.AvisoGeneralDto
import com.cadeteria.cadete.data.remote.dto.CadeteConfigDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CartelesInicioTest {

    private fun config(activo: Boolean?, titulo: String?, textos: List<String>?) = CadeteConfigDto(
        frecuenciaUbicacionSeg = 45, cloudinaryCloudName = "", cloudinaryUploadPreset = "",
        versionMinimaApp = 1, firmaReceptorObligatoria = false,
        pagoSemanalMonto = 0.0, comisionPorcentaje = 0.0, creditoBajoAlertaUmbral = 0.0,
        recordatoriosActivo = activo, recordatoriosTitulo = titulo, recordatoriosTextos = textos,
    )

    @Test
    fun `sin configuracion o backend viejo salen los de siempre`() {
        assertEquals(CartelesInicio.TEXTOS_POR_DEFECTO, CartelesInicio.recordatorios(null)!!.textos)
        val viejo = CartelesInicio.recordatorios(config(null, null, null))!!
        assertEquals("Antes de arrancar", viejo.titulo)
        assertEquals(3, viejo.textos.size)
    }

    @Test
    fun `los del panel, sin renglones vacios`() {
        val r = CartelesInicio.recordatorios(config(true, " Hoy ", listOf("Casco", " ", "Chaleco")))!!
        assertEquals("Hoy", r.titulo)
        assertEquals(listOf("Casco", "Chaleco"), r.textos)
    }

    @Test
    fun `apagado o sin renglones no hay cartel`() {
        assertNull(CartelesInicio.recordatorios(config(false, "Hoy", listOf("Casco"))))
        assertNull(CartelesInicio.recordatorios(config(true, "Hoy", emptyList())))
    }

    @Test
    fun `la cola no repite y va del mas viejo al mas nuevo`() {
        val a = AvisoGeneralDto("a", "viejo")
        val b = AvisoGeneralDto("b", "medio")
        val c = AvisoGeneralDto("c", "nuevo")

        // El backend los manda del más nuevo al más viejo; "a" ya había llegado en vivo.
        val cola = CartelesInicio.encolar(listOf(a), listOf(c, b, a))

        assertEquals(listOf("a", "b", "c"), cola.map { it.id })
    }

    @Test
    fun `titulo del aviso`() {
        assertEquals("Aviso de la cadetería", CartelesInicio.tituloAviso(1))
        assertEquals("Aviso (3 sin leer)", CartelesInicio.tituloAviso(3))
    }
}
