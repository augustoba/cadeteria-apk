package com.cadeteria.cadete.data.remote.dto

data class CadeteResumenDto(
    val id: String,
    val nombre: String,
    val apellido: String,
    val fotoUrl: String?,
)

/** Espeja PedidoDtos.PedidoResponse — es la respuesta tanto de lista como de detalle. */
data class PedidoDto(
    val id: String,
    val numero: Long,
    val clienteTelefono: String,
    val clienteNombre: String,
    val origenDireccion: String,
    val origenLat: Double,
    val origenLng: Double,
    val destinoDireccion: String,
    val destinoLat: Double,
    val destinoLng: Double,
    val precio: Double,
    val montoDeclarado: Double?,
    val detalle: String?,
    val zona: LookupDto,
    val tipoVehiculoRequerido: LookupDto,
    val estado: LookupDto,
    val cadeteAsignado: CadeteResumenDto?,
    val programado: Boolean,
    val fechaProgramada: String?,
    val creadoEn: String?,
    val asignadoEn: String?,
    val aceptadoEn: String?,
    val retiradoEn: String?,
    val finalizadoEn: String?,
    val fotoRecepcionUrl: String?,
    val entregaReceptorNombre: String?,
    val entregaFotoUrl: String?,
    val tokenSeguimiento: String,
    /** Paradas intermedias, en orden (repartos con varias entregas en la misma vuelta) — vacío si el pedido es simple. */
    val paradas: List<ParadaDto> = emptyList(),
)

/** Espeja PedidoDtos.ParadaResponse. */
data class ParadaDto(
    val id: String,
    val orden: Int,
    val direccion: String,
    val lat: Double,
    val lng: Double,
    val entregadoEn: String?,
)

/** Botón "Retirado": la foto es opcional. lat/lng: ubicación del cadete en ese momento (opcional). */
data class RecepcionRequest(val fotoUrl: String?, val lat: Double? = null, val lng: Double? = null)

/** Botón "Rechazar": motivo opcional, texto libre (para detectar patrones en Métricas). */
data class RechazarRequest(val motivo: String?)

/** Botón "No se pudo entregar" (ej. el cliente no atendió): motivo opcional, texto libre. */
data class NoEntregadoRequest(val motivo: String?)

data class FinalizarRequest(
    val receptorNombre: String?,
    val fotoUrl: String?,
    val firmaUrl: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

/** Nota de texto libre sobre el viaje (ej. "entregado en porteria a Fulano") — se ve en el detalle del panel. */
data class ComentarioRequest(val texto: String)

/** Sección "Finalizados" de la app — espeja PedidoDtos.HistorialResponse del backend. */
data class HistorialDto(
    val pedidos: List<PedidoDto>,
    val cantidadViajes: Int,
    val montoTotal: Double,
    val cantidadRechazados: Long,
    val cantidadNoAceptados: Long,
)

/** Cuerpo de los mensajes que llegan por /queue/cadete/{id}/viajes (WebSocketPublisher.EventoViaje). */
data class EventoViajeDto(
    val tipo: String,
    val pedido: PedidoDto,
)

object EventoViaje {
    const val VIAJE_ASIGNADO = "VIAJE_ASIGNADO"
    const val VIAJE_QUITADO = "VIAJE_QUITADO"
    const val VIAJE_CANCELADO = "VIAJE_CANCELADO"
}

/**
 * Cuerpo de los mensajes que llegan por /queue/cadete/{id}/avisos (aviso general o
 * recordatorio de demora). `avisoId` solo viene en los avisos generales — permite
 * confirmarle al backend que se vio (los recordatorios de demora no se confirman).
 */
data class AvisoDto(val mensaje: String, val avisoId: String? = null)

object EstadoPedido {
    const val SIN_ASIGNAR = "SIN_ASIGNAR"
    const val PENDIENTE = "PENDIENTE"
    const val EN_CURSO = "EN_CURSO"
    const val FINALIZADO = "FINALIZADO"
    const val CANCELADO = "CANCELADO"
    const val PROGRAMADO = "PROGRAMADO"
}
