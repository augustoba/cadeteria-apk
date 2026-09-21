package com.cadeteria.cadete.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La decisión de alertar estaba enterrada dentro del LaunchedEffect del contador, donde no se
 * podía probar. Extraída acá, el caso que importa — "que NO vuelva a sonar en el mismo umbral" —
 * queda cubierto: sin ese chequeo el teléfono sonaría en cada tick de cada segundo del umbral.
 */
class AlertasContadorTest {

    @Test
    fun `alerta en los umbrales`() {
        assertTrue(tocaAlertar(30, emptySet()))
        assertTrue(tocaAlertar(15, emptySet()))
        assertTrue(tocaAlertar(5, emptySet()))
    }

    @Test
    fun `no alerta fuera de los umbrales`() {
        assertFalse(tocaAlertar(29, emptySet()))
        assertFalse(tocaAlertar(16, emptySet()))
        assertFalse(tocaAlertar(0, emptySet()))
    }

    @Test
    fun `no vuelve a alertar en un umbral ya avisado`() {
        assertFalse(tocaAlertar(30, setOf(30)))
        assertTrue(tocaAlertar(15, setOf(30)))
    }
}
