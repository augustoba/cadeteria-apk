package com.cadeteria.cadete.data.remote

import com.cadeteria.cadete.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega el JWT guardado a cada request — corre en el thread de OkHttp, no en el principal.
 * Desde la versión 3 también manda la versión de la app (X-App-Version): el servidor la usa para
 * exigir la versión mínima al activarse, no solo al iniciar sesión.
 */
class AuthInterceptor(private val session: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { session.tokenSync() }
        val original = chain.request().newBuilder()
            .header("X-App-Version", com.cadeteria.cadete.BuildConfig.VERSION_CODE.toString())
            .build()
        if (token.isNullOrBlank()) return chain.proceed(original)
        val autenticado = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(autenticado)
    }
}
