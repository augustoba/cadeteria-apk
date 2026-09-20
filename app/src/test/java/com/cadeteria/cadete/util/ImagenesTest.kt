package com.cadeteria.cadete.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagenesTest {

    private val conVersion = "https://res.cloudinary.com/demo/image/upload/v1234567/foto.jpg"

    @Test
    fun `inserta la transformación antes de la versión`() {
        assertEquals(
            "https://res.cloudinary.com/demo/image/upload/f_auto,q_auto,c_limit,w_120/v1234567/foto.jpg",
            optimizarImagen(conVersion, 120),
        )
    }

    @Test
    fun `no duplica si ya trae transformacion`() {
        val ya = "https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/v1234567/foto.jpg"
        assertEquals(ya, optimizarImagen(ya, 120))
    }

    @Test
    fun `deja igual una url que no es de cloudinary`() {
        val otra = "https://ejemplo.com/foto.jpg"
        assertEquals(otra, optimizarImagen(otra, 120))
    }

    @Test
    fun `deja igual una url de cloudinary sin version ni transformacion`() {
        // Sin un segmento reconocible después de /upload/, insertar sería adivinar.
        val rara = "https://res.cloudinary.com/demo/image/upload/foto.jpg"
        assertEquals(rara, optimizarImagen(rara, 120))
    }

    @Test
    fun `deja igual una url firmada (insertar delante de la firma la romperia)`() {
        val firmada = "https://res.cloudinary.com/demo/image/upload/s--abc123--/v1234567/foto.jpg"
        assertEquals(firmada, optimizarImagen(firmada, 120))
    }

    @Test
    fun `devuelve null tal cual`() {
        assertNull(optimizarImagen(null, 120))
    }

    @Test
    fun `respeta el ancho que se le pasa`() {
        assertTrue(optimizarImagen(conVersion, 1600).orEmpty().contains("w_1600"))
        assertTrue(optimizarImagen(conVersion, 240).orEmpty().contains("w_240"))
    }
}
