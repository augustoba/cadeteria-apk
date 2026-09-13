package com.cadeteria.cadete.data.remote.dto

/**
 * GeoJSON que devuelve OpenRouteService (proxeado tal cual por GET /pedidos/me/{id}/ruta,
 * spec 5.7) — solo modelamos lo que usamos: la lista de puntos [lng, lat] de la ruta.
 */
data class RutaResponseDto(
    val features: List<RutaFeatureDto>?,
)

data class RutaFeatureDto(
    val geometry: RutaGeometryDto?,
)

data class RutaGeometryDto(
    val coordinates: List<List<Double>>?,
)
