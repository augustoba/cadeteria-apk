package com.cadeteria.cadete.ui.viaje

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Finalizar con señal mala (2026-10-19): todo lo que no es un rechazo del servidor se guarda en la
 * cola y el viaje queda como finalizado, para que el cadete no lo vuelva a hacer.
 */
class FallaDeSenalTest {

    private fun http(codigo: Int) = HttpException(Response.error<Any>(codigo, "{}".toResponseBody()))

    @Test
    fun sinConexionTimeoutY5xxSeGuardanEnLaCola() {
        assertTrue(java.io.IOException("sin red").esFallaDeSenal())
        assertTrue(java.net.SocketTimeoutException().esFallaDeSenal())
        for (codigo in listOf(408, 429, 500, 502, 504)) assertTrue("código $codigo", http(codigo).esFallaDeSenal())
    }

    @Test
    fun unaRespuestaQueNoEsDelBackendSeGuardaEnLaCola() {
        // Portal de wifi o proxy que devuelve HTML: Gson no lo puede leer.
        assertTrue(com.google.gson.JsonSyntaxException("no es JSON").esFallaDeSenal())
    }

    @Test
    fun unRechazoDelServidorSeMuestra() {
        for (codigo in listOf(400, 404, 409, 422)) assertFalse("código $codigo", http(codigo).esFallaDeSenal())
    }

    @Test
    fun laFotoSeReintentaSalvoCloudinarySinConfigurar() {
        assertTrue(Exception("Cloudinary devolvió 502: Bad Gateway").esFallaDeSubidaPasajera())
        assertTrue(java.io.IOException().esFallaDeSubidaPasajera())
        assertFalse(IllegalStateException("Cloudinary no está configurado").esFallaDeSubidaPasajera())
    }
}
