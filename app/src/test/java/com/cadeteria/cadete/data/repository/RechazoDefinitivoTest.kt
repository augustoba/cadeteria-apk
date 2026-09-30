package com.cadeteria.cadete.data.repository

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Cola sin señal (2026-09-29): como el viaje queda bloqueado mientras tiene algo encolado, lo que el
 * servidor rechaza para siempre tiene que salir de la cola (si no, el viaje queda trabado).
 */
class RechazoDefinitivoTest {

    private fun http(codigo: Int, cuerpo: String = "{}") =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody()))

    @Test
    fun sinSenalNoEsDefinitivo() {
        assertFalse(PendingActionsRepository.esRechazoDefinitivo(java.io.IOException("sin red")))
    }

    @Test
    fun erroresDelServidorOSesionNoSonDefinitivos() {
        for (codigo in listOf(401, 403, 408, 429, 500, 502, 503)) {
            assertFalse("código $codigo", PendingActionsRepository.esRechazoDefinitivo(http(codigo)))
        }
    }

    @Test
    fun rechazosDeNegocioSonDefinitivos() {
        for (codigo in listOf(400, 404, 409, 422)) {
            assertTrue("código $codigo", PendingActionsRepository.esRechazoDefinitivo(http(codigo)))
        }
    }

    @Test
    fun elMotivoSaleDelCuerpoDelError() {
        val e = http(400, """{"message":"Todavía no pasaron 10 minutos desde el retiro."}""")
        assertEquals("Todavía no pasaron 10 minutos desde el retiro.", PendingActionsRepository.motivoDelRechazo(e))
    }
}
