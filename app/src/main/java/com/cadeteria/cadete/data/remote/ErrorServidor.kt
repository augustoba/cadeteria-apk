package com.cadeteria.cadete.data.remote

import org.json.JSONObject
import retrofit2.HttpException

/**
 * Mensaje que mandó el backend en el cuerpo del error (`ApiError.message`, 2026-09-26) — ej. "La
 * oferta de este viaje ya no está vigente." o "Cuenta bloqueada temporalmente…". Null si no fue un
 * error HTTP (sin conexión) o si no vino un mensaje legible.
 */
fun Throwable.mensajeDelServidor(): String? {
    val http = this as? HttpException ?: return null
    val cuerpo = runCatching { http.response()?.errorBody()?.string() }.getOrNull() ?: return null
    return runCatching { JSONObject(cuerpo).optString("message") }.getOrNull()?.takeIf { it.isNotBlank() }
}

/** Código HTTP del error, o null si no llegó a haber respuesta (sin conexión, timeout). */
fun Throwable.codigoHttp(): Int? = (this as? HttpException)?.code()
