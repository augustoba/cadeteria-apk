package com.cadeteria.cadete.data.remote

import com.cadeteria.cadete.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Si el backend devuelve 401 (token vencido, o sesión tomada por otro dispositivo — ver
 * JwtAuthFilter/claim "sid"), cierra la sesión local y avisa a la UI para volver al login.
 * No aplica al propio login: una contraseña incorrecta también devuelve 401 y ahí no hay
 * ninguna sesión que cerrar.
 */
class SesionInvalidaInterceptor(
    private val session: SessionManager,
    private val onSesionInvalida: () -> Unit,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && !request.url.encodedPath.contains("/api/auth/login/")) {
            runBlocking { session.cerrarSesion() }
            onSesionInvalida()
        }
        return response
    }
}
