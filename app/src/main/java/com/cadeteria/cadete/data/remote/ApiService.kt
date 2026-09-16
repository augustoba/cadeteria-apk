package com.cadeteria.cadete.data.remote

import com.cadeteria.cadete.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Espeja uno a uno los endpoints de com.cadeteria.backend.controller.* que puede usar
 * la app de cadetes (login, api/cadetes/me/..., api/pedidos/me/..., api/chat/...).
 * No incluye nada de api/admin/... — eso es del panel, no de esta app.
 */
interface ApiService {

    @POST("api/auth/login/cadete")
    suspend fun login(@Body req: LoginRequest): TokenResponse

    @GET("api/cadetes/me")
    suspend fun miPerfil(): CadeteDto

    @GET("api/cadetes/me/configuracion")
    suspend fun miConfiguracion(): CadeteConfigDto

    /** Cuánto facturó el cadete en la semana en curso y si ya se le pagó. */
    @GET("api/cadetes/me/pago-semanal")
    suspend fun miPagoSemanal(): MiSemanaDto

    /** Avisos generales que todavía no vio — para no perderse los que llegaron desconectado (ronda 10, punto 98). */
    @GET("api/cadetes/me/avisos/pendientes")
    suspend fun avisosPendientes(): List<AvisoGeneralDto>

    /** Confirma que el cadete vio un aviso general. */
    @POST("api/cadetes/me/avisos/{id}/leido")
    suspend fun marcarAvisoLeido(@Path("id") id: String)

    /** Pantalla "Avisos" con historial (mejora 2026-09-16) — a diferencia de [avisosPendientes], incluye los ya leídos. */
    @GET("api/cadetes/me/avisos/historial")
    suspend fun historialAvisos(): List<AvisoHistorialDto>

    @PATCH("api/cadetes/me/estado")
    suspend fun actualizarEstado(@Body req: EstadoRequest): CadeteDto

    @PATCH("api/cadetes/me/ubicacion")
    suspend fun actualizarUbicacion(@Body req: UbicacionRequest): CadeteDto

    @PATCH("api/cadetes/me/fcm-token")
    suspend fun actualizarFcmToken(@Body req: FcmTokenRequest): CadeteDto

    @PATCH("api/cadetes/me/password")
    suspend fun cambiarPassword(@Body req: CambiarPasswordRequest): CadeteDto

    @PATCH("api/cadetes/me/telefono")
    suspend fun actualizarTelefono(@Body req: TelefonoRequest): CadeteDto

    @PATCH("api/cadetes/me/cuenta")
    suspend fun actualizarCuenta(@Body req: CuentaRequest): CadeteDto

    @GET("api/pedidos/me/activo")
    suspend fun viajeActivo(): retrofit2.Response<PedidoDto>

    /** Sección "Asignados y en curso" — a diferencia de viajeActivo(), no asume uno solo. */
    @GET("api/pedidos/me/activos")
    suspend fun viajesActivos(): List<PedidoDto>

    /** Sección "Finalizados": listado + resumen. desde/hasta en yyyy-MM-dd — null = todo el historial. */
    @GET("api/pedidos/me/historial")
    suspend fun historial(@Query("desde") desde: String?, @Query("hasta") hasta: String?): HistorialDto

    @GET("api/pedidos/me/{id}")
    suspend fun detallePedido(@Path("id") id: String): PedidoDto

    @POST("api/pedidos/me/{id}/aceptar")
    suspend fun aceptarViaje(@Path("id") id: String): PedidoDto

    @POST("api/pedidos/me/{id}/rechazar")
    suspend fun rechazarViaje(@Path("id") id: String, @Body req: RechazarRequest): PedidoDto

    /** Botón "Retirado" — foto opcional. */
    @POST("api/pedidos/me/{id}/recepcion")
    suspend fun marcarRetirado(@Path("id") id: String, @Body req: RecepcionRequest): PedidoDto

    @POST("api/pedidos/me/{id}/finalizar")
    suspend fun finalizarViaje(@Path("id") id: String, @Body req: FinalizarRequest): PedidoDto

    /** Botón "No se pudo entregar" (ej. el cliente no atendió) — el pedido no se anula. */
    @POST("api/pedidos/me/{id}/no-entregado")
    suspend fun marcarNoEntregado(@Path("id") id: String, @Body req: NoEntregadoRequest): PedidoDto

    /** Marca una parada intermedia como entregada (repartos con varias entregas en la misma vuelta). */
    @POST("api/pedidos/me/{id}/paradas/{paradaId}/entregada")
    suspend fun marcarParadaEntregada(@Path("id") id: String, @Path("paradaId") paradaId: String): PedidoDto

    @GET("api/pedidos/me/{id}/ruta")
    suspend fun ruta(@Path("id") id: String): RutaResponseDto

    /** Nota de texto libre sobre el viaje (ej. "entregado en porteria a Fulano"). */
    @POST("api/pedidos/me/{id}/comentarios")
    suspend fun agregarComentario(@Path("id") id: String, @Body req: ComentarioRequest)

    @GET("api/chat/{cadeteId}")
    suspend fun historialChat(@Path("cadeteId") cadeteId: String): List<MensajeDto>

    @POST("api/chat/{cadeteId}/mensajes")
    suspend fun enviarMensaje(@Path("cadeteId") cadeteId: String, @Body req: MensajeRequest): MensajeDto

    @PATCH("api/chat/{cadeteId}/leido")
    suspend fun marcarLeido(@Path("cadeteId") cadeteId: String)
}
