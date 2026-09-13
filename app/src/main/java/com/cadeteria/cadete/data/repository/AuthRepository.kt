package com.cadeteria.cadete.data.repository

import com.cadeteria.cadete.BuildConfig
import com.cadeteria.cadete.data.local.SessionManager
import com.cadeteria.cadete.data.remote.RetrofitProvider
import com.cadeteria.cadete.data.remote.dto.LoginRequest

/** Se lanza si el APK instalado quedó por debajo de `version_minima_app` (spec: distribución por Bluetooth, sin Play Store). */
class VersionDesactualizadaException : Exception("Esta versión de la app quedó vieja — pedile al admin el APK actualizado.")

class AuthRepository(
    private val retrofitProvider: RetrofitProvider,
    private val session: SessionManager,
) {
    /** Loguea y de paso resuelve el perfil, para tener id/nombre a mano (chat, saludo, etc). */
    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val api = retrofitProvider.apiService()
        val token = api.login(LoginRequest(username.trim(), password))
        session.guardarSesion(token.token, username.trim(), cadeteId = "", cadeteNombre = "")

        // Con el token ya guardado, el interceptor de auth ahora puede pedir el perfil.
        val perfil = retrofitProvider.apiService().miPerfil()
        session.guardarSesion(token.token, username.trim(), perfil.id, perfil.nombre)

        val config = runCatching { retrofitProvider.apiService().miConfiguracion() }.getOrNull()
        if (config != null && BuildConfig.VERSION_CODE < config.versionMinimaApp) {
            session.cerrarSesion()
            throw VersionDesactualizadaException()
        }
    }

    suspend fun logout() {
        session.cerrarSesion()
    }

    suspend fun isLoggedIn(): Boolean = session.isLoggedIn()
}
