package com.cadeteria.cadete.data.remote.dto

data class MensajeRequest(val texto: String?, val audioUrl: String? = null, val imagenUrl: String? = null)

/** Espeja ChatDtos.MensajeResponse. `autor` es "ADMIN" o "CADETE". */
data class MensajeDto(
    val id: String,
    val autor: String,
    val texto: String?,
    val audioUrl: String?,
    val imagenUrl: String?,
    val enviadoEn: String,
    val leido: Boolean,
)

object AutorMensaje {
    const val ADMIN = "ADMIN"
    const val CADETE = "CADETE"
}
