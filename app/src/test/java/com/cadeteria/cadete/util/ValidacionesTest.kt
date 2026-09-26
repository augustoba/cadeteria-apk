package com.cadeteria.cadete.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mismas reglas que el backend (2026-09-26). */
class ValidacionesTest {

    @Test
    fun `nombre de quien recibe solo letras`() {
        listOf("Lucía Pérez", "O'Connor", "Jean-Pierre").forEach { assertTrue(it, Validaciones.NOMBRE_PERSONA.matches(it)) }
        listOf("Lucía 2", "123", "J@n").forEach { assertFalse(it, Validaciones.NOMBRE_PERSONA.matches(it)) }
    }

    @Test
    fun `patente de moto vieja o nueva`() {
        listOf("123ABC", "a123bcd", "A 123 BCD").forEach { assertTrue(it, Validaciones.PATENTE_MOTO.matches(it)) }
        listOf("ABC123", "AB123CD", "12ABC").forEach { assertFalse(it, Validaciones.PATENTE_MOTO.matches(it)) }
        assertEquals("A123BCD", Validaciones.normalizarPatente("a 123-bcd"))
    }

    @Test
    fun `telefono cbu alias y contraseña`() {
        assertTrue(Validaciones.TELEFONO.matches("+54 9 381 555-1234"))
        assertFalse(Validaciones.TELEFONO.matches("381abc1234"))
        assertTrue(Validaciones.CBU.matches("0000003100000000000001"))
        assertFalse(Validaciones.CBU.matches("123"))
        assertTrue(Validaciones.ALIAS_CBU.matches("juan.perez.mp"))
        assertFalse(Validaciones.passwordValida("123"))
        assertTrue(Validaciones.vacioO(Validaciones.CBU, ""))
    }

    @Test
    fun `problemas junta los mensajes`() {
        assertEquals("b · c", Validaciones.problemas(false to "a", true to "b", true to "c"))
        assertNull(Validaciones.problemas(false to "a"))
    }
}
