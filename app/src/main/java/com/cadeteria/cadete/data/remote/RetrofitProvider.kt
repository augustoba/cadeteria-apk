package com.cadeteria.cadete.data.remote

import com.cadeteria.cadete.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Reconstruye el `ApiService` cada vez que cambia la URL del backend (configurable
 * desde la app, ver SessionManager) — para no recrear el cliente en cada llamada,
 * cachea la última instancia mientras la URL no cambie.
 */
class RetrofitProvider(private val session: SessionManager, private val onSesionInvalida: () -> Unit = {}) {

    @Volatile private var cachedBaseUrl: String? = null
    @Volatile private var cachedApi: ApiService? = null

    suspend fun apiService(): ApiService {
        val baseUrl = session.baseUrlSync()
        val current = cachedApi
        if (current != null && cachedBaseUrl == baseUrl) return current

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .addInterceptor(SesionInvalidaInterceptor(session, onSesionInvalida))
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val normalizado = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizado)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)
        cachedBaseUrl = baseUrl
        cachedApi = api
        return api
    }

    /** Host:puerto actual, para armar la URL del WebSocket (ver realtime/StompClient). */
    suspend fun httpBaseUrl(): String = session.baseUrlSync()
}
