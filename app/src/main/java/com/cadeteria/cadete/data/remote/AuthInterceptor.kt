package com.cadeteria.cadete.data.remote

import com.cadeteria.cadete.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Agrega el JWT guardado a cada request — corre en el thread de OkHttp, no en el principal. */
class AuthInterceptor(private val session: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { session.tokenSync() }
        val original = chain.request()
        if (token.isNullOrBlank()) return chain.proceed(original)
        val autenticado = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(autenticado)
    }
}
