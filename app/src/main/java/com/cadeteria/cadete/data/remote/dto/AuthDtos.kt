package com.cadeteria.cadete.data.remote.dto

data class LoginRequest(
    val username: String,
    val password: String,
    /** Para que el panel admin vea qué versión de APK tiene cada cadete conectado (mejora 2026-09-17). */
    val versionApp: Int? = null,
)

data class TokenResponse(
    val token: String,
    val tokenType: String,
    val tipo: String,
    val expiresAt: String,
)

data class RecuperarPasswordRequest(
    val username: String,
)

data class ConfirmarRecuperarPasswordRequest(
    val username: String,
    val codigo: String,
    val nuevaPassword: String,
)
