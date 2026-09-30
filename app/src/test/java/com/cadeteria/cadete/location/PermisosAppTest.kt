package com.cadeteria.cadete.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Permisos obligatorios (2026-09-29): sin los 4 no se usa la app. */
class PermisosAppTest {

    @Test
    fun conTodoNoFaltaNada() {
        assertTrue(PermisosApp.faltantes(ubicacion = true, notificaciones = true, microfono = true, bateria = true).isEmpty())
    }

    @Test
    fun faltanEnElOrdenDeLaPantalla() {
        assertEquals(
            listOf(PermisoApp.UBICACION, PermisoApp.NOTIFICACIONES, PermisoApp.MICROFONO, PermisoApp.BATERIA),
            PermisosApp.faltantes(ubicacion = false, notificaciones = false, microfono = false, bateria = false),
        )
        assertEquals(listOf(PermisoApp.BATERIA), PermisosApp.faltantes(true, true, true, false))
    }

    @Test
    fun loQueSeLeInformaAlPanelSonLasClaves() {
        assertEquals("NOTIFICACIONES,BATERIA", PermisosApp.paraInformar(listOf(PermisoApp.NOTIFICACIONES, PermisoApp.BATERIA)))
        assertEquals("", PermisosApp.paraInformar(emptyList()))
    }
}
