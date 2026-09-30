package com.cadeteria.cadete.data.remote.dto

/** Permisos que le faltan a la app, separados por coma ("NOTIFICACIONES,BATERIA"); vacío = todos dados (2026-09-29). */
data class PermisosRequest(val faltantes: String)

/** Link de descarga de un solo uso de la APK actual (2026-09-29), para "Tu versión es vieja". */
data class LinkApkDto(val url: String?, val venceEn: String?)

data class LoginRequest(
    val username: String,
    val password: String,
    /** Para que el panel admin vea qué versión de APK tiene cada cadete conectado (mejora 2026-09-17). */
    val versionApp: Int? = null,
    /** Un celular por cadete (2026-09-29): identificador de este celular (ANDROID_ID) y su marca y modelo. */
    val celularId: String? = null,
    val celularModelo: String? = null,
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
