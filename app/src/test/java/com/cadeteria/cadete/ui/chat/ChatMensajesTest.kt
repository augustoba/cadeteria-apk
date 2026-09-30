package com.cadeteria.cadete.ui.chat

import com.cadeteria.cadete.data.remote.dto.MensajeDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Bug 2026-09-29: al enviar (texto, audio o foto) la app se cerraba. El mensaje llegaba dos veces —
 * la respuesta del POST y el mismo mensaje en vivo por WebSocket— y la lista (clave = id) se caía
 * con dos ítems de la misma clave.
 */
class ChatMensajesTest {

    private fun msj(id: String, texto: String = "hola") =
        MensajeDto(id, "CADETE", texto, null, null, "2026-09-29T21:00:00Z", false)

    @Test
    fun agregaUnoNuevo() {
        assertEquals(listOf("a", "b"), ChatMensajes.agregar(listOf(msj("a")), msj("b")).map { it.id })
    }

    @Test
    fun siYaLlegoPorWebSocketNoLoDuplica() {
        val lista = listOf(msj("a"), msj("b"))
        assertEquals(listOf("a", "b"), ChatMensajes.agregar(lista, msj("b")).map { it.id })
    }
}
