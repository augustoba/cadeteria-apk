package com.cadeteria.cadete.data.repository

import com.cadeteria.cadete.BuildConfig
import com.cadeteria.cadete.data.local.SessionManager
import com.cadeteria.cadete.data.remote.RetrofitProvider
import com.cadeteria.cadete.data.remote.dto.ConfirmarRecuperarPasswordRequest
import com.cadeteria.cadete.data.remote.dto.LoginRequest
import com.cadeteria.cadete.data.remote.dto.RecuperarPasswordRequest

/** Se lanza si el APK instalado quedó por debajo de `version_minima_app` (spec: distribución por Bluetooth, sin Play Store). */
class VersionDesactualizadaException : Exception("Esta versión de la app quedó vieja — pedile al admin el APK actualizado.")

class AuthRepository(
    private val retrofitProvider: RetrofitProvider,
    private val session: SessionManager,
) {
    /** Loguea y de paso resuelve el perfil, para tener id/nombre a mano (chat, saludo, etc). */
    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val api = retrofitProvider.apiService()
        val token = api.login(LoginRequest(username.trim(), password, BuildConfig.VERSION_CODE))
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

    /** Sin sesión — el backend responde OK siempre, exista o no ese usuario (no revela qué DNIs están de alta). */
    suspend fun recuperarPassword(username: String): Result<Unit> = runCatching {
        retrofitProvider.apiService().recuperarPassword(RecuperarPasswordRequest(username.trim()))
    }

    suspend fun confirmarRecuperarPassword(username: String, codigo: String, nuevaPassword: String): Result<Unit> = runCatching {
        retrofitProvider.apiService().confirmarRecuperarPassword(
            ConfirmarRecuperarPasswordRequest(username.trim(), codigo.trim(), nuevaPassword),
        )
    }

    suspend fun isLoggedIn(): Boolean = session.isLoggedIn()
}
