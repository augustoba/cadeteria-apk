package com.cadeteria.cadete.data.remote.dto

/**
 * "Avisos de la calle" (carril C, 2026-09-28): espeja AvisoCalleDtos del backend. Llega por
 * /queue/cadete/{id}/calle y por GET /api/cadetes/me/avisos-calle. A los cadetes no les dice quién avisó.
 */
data class AvisoCalleDto(
    val id: String,
    val tipo: String,
    val tipoTexto: String,
    val lat: Double,
    val lng: Double,
    /** "Mate de Luna 2400" — null si no se pudo saber la calle. */
    val calle: String?,
    val creadoEn: String,
    val venceEn: String,
)

data class AvisoCalleRequest(val tipo: String, val lat: Double, val lng: Double, val precision: Float?)

/** Las 4 opciones del botón "Avisar": clave que viaja al backend, emoji y texto. */
object TipoAvisoCalle {
    const val CONTROL = "CONTROL"
    const val CALLE_CORTADA = "CALLE_CORTADA"
    const val ACCIDENTE = "ACCIDENTE"
    const val PIQUETE = "PIQUETE"

    val OPCIONES = listOf(
        Triple(CONTROL, "🚓", "Control"),
        Triple(CALLE_CORTADA, "🚧", "Calle cortada"),
        Triple(ACCIDENTE, "💥", "Accidente"),
        Triple(PIQUETE, "✊", "Piquete"),
    )

    fun emoji(tipo: String): String = OPCIONES.firstOrNull { it.first == tipo }?.second ?: "🚨"
}
