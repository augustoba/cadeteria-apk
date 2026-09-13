package com.cadeteria.cadete.data.remote.dto

/** Respuesta común del backend para tablas de parametría (tipo_vehiculo, estado, zona). */
data class LookupDto(
    val id: String,
    val nombre: String,
)
