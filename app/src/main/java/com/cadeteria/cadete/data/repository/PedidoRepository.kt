package com.cadeteria.cadete.data.repository

import com.cadeteria.cadete.data.remote.RetrofitProvider
import com.cadeteria.cadete.data.remote.dto.ComentarioRequest
import com.cadeteria.cadete.data.remote.dto.ReporteClienteRequest
import com.cadeteria.cadete.data.remote.dto.FinalizarRequest
import com.cadeteria.cadete.data.remote.dto.HistorialDto
import com.cadeteria.cadete.data.remote.dto.NoEntregadoRequest
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.data.remote.dto.RecepcionRequest
import com.cadeteria.cadete.data.remote.dto.RechazarRequest
import com.cadeteria.cadete.data.remote.dto.RutaResponseDto

class PedidoRepository(private val retrofitProvider: RetrofitProvider) {

    /** null si no hay viaje asignado ahora mismo (backend devuelve 204). */
    suspend fun viajeActivo(): Result<PedidoDto?> = runCatching {
        val resp = retrofitProvider.apiService().viajeActivo()
        if (resp.code() == 204) null else resp.body()
    }

    /** Sección "Asignados y en curso" — puede haber más de uno si el cadete tiene tope > 1. */
    suspend fun viajesActivos(): Result<List<PedidoDto>> =
        runCatching { retrofitProvider.apiService().viajesActivos() }

    /**
     * Sección "Finalizados": listado + resumen (viajes, monto total, rechazos, no-aceptados).
     * desde/hasta en yyyy-MM-dd — null = todo el historial (auditoría UX 2026-09-13, punto 5).
     */
    suspend fun historial(desde: String? = null, hasta: String? = null): Result<HistorialDto> =
        runCatching { retrofitProvider.apiService().historial(desde, hasta) }

    /** Viajes y facturado de un día (yyyy-MM-dd), sin la lista — estadísticas de Inicio. */
    suspend fun resumenDelDia(dia: String): Result<HistorialDto> =
        runCatching { retrofitProvider.apiService().resumenHistorial(dia, dia) }

    suspend fun minutosConectadoHoy(): Result<Long> =
        runCatching { retrofitProvider.apiService().conectadoHoy().minutos }

    suspend fun detalle(id: String): Result<PedidoDto> =
        runCatching { retrofitProvider.apiService().detallePedido(id) }

    suspend fun aceptar(id: String): Result<PedidoDto> =
        runCatching { retrofitProvider.apiService().aceptarViaje(id) }

    suspend fun rechazar(id: String, motivo: String? = null): Result<PedidoDto> =
        runCatching { retrofitProvider.apiService().rechazarViaje(id, RechazarRequest(motivo)) }

    /** Botón "Retirado" — foto opcional (null si el cadete no sacó ninguna). lat/lng opcionales (sin GPS disponible). */
    suspend fun marcarRetirado(
        id: String,
        fotoUrl: String?,
        lat: Double? = null,
        lng: Double? = null,
        archivoPerdido: Boolean = false,
        precision: Float? = null,
        calleDetectada: String? = null,
        localidadDetectada: String? = null,
    ): Result<PedidoDto> =
        runCatching {
            retrofitProvider.apiService().marcarRetirado(
                id, RecepcionRequest(fotoUrl, lat, lng, archivoPerdido, precision, calleDetectada, localidadDetectada),
            )
        }

    suspend fun finalizar(
        id: String,
        receptorNombre: String?,
        fotoUrl: String?,
        firmaUrl: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        archivoPerdido: Boolean = false,
        precision: Float? = null,
        calleDetectada: String? = null,
        localidadDetectada: String? = null,
    ): Result<PedidoDto> =
        runCatching {
            retrofitProvider.apiService().finalizarViaje(
                id,
                FinalizarRequest(receptorNombre, fotoUrl, firmaUrl, lat, lng, archivoPerdido, precision, calleDetectada, localidadDetectada),
            )
        }

    /** Botón "No se pudo entregar" (ej. el cliente no atendió) — el pedido no se anula, lo puede reintentar el admin. */
    suspend fun marcarNoEntregado(id: String, motivo: String? = null): Result<PedidoDto> =
        runCatching { retrofitProvider.apiService().marcarNoEntregado(id, NoEntregadoRequest(motivo)) }

    /** Marca una parada intermedia como entregada (repartos con varias entregas en la misma vuelta). */
    suspend fun marcarParadaEntregada(id: String, paradaId: String): Result<PedidoDto> =
        runCatching { retrofitProvider.apiService().marcarParadaEntregada(id, paradaId) }

    /** null si no se pudo calcular (sin ubicación propia todavía, o ORS sin configurar) — nunca falla. */
    suspend fun ruta(id: String): Result<RutaResponseDto?> =
        runCatching { retrofitProvider.apiService().ruta(id) }
            .fold(onSuccess = { Result.success(it) }, onFailure = { Result.success(null) })

    /** Nota de texto libre sobre el viaje (ej. "entregado en porteria a Fulano"). */
    suspend fun agregarComentario(id: String, texto: String): Result<Unit> =
        runCatching { retrofitProvider.apiService().agregarComentario(id, ComentarioRequest(texto)) }

    suspend fun reportarCliente(id: String, tipo: String, nota: String?): Result<Unit> =
        runCatching { retrofitProvider.apiService().reportarCliente(id, ReporteClienteRequest(tipo, nota)) }
}
