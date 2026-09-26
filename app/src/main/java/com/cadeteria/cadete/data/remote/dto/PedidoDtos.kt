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
    /** Transporta objetos de valor (no dinero) — declarado por el cliente. */
    val llevaValores: Boolean = false,
    /** Valor declarado de esos objetos, null si no lleva. */
    val montoValores: Double? = null,
    /**
     * Detalle, piso, depto y observaciones de cada dirección: el backend los manda en null
     * mientras la oferta no está aceptada (mejora 2026-09-24) — aparecen al aceptar.
     */
    val detalle: String?,
    val origenPiso: String? = null,
    val origenDepto: String? = null,
    val origenObservaciones: String? = null,
    val destinoPiso: String? = null,
    val destinoDepto: String? = null,
    val destinoObservaciones: String? = null,
    val requiereMoto: Boolean,
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
    /** Último reclamo del cliente desde el seguimiento (2026-09-25) — null si no reclamó. */
    val reclamoDetalle: String? = null,
    val reclamoEn: String? = null,
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
data class RecepcionRequest(
    val fotoUrl: String?,
    val lat: Double? = null,
    val lng: Double? = null,
    /** La foto se sacó pero el archivo local se perdió antes de subirse (cola offline) — el backend lo acepta igual. */
    val archivoPerdido: Boolean = false,
    /** Error del GPS en metros: con buena precisión el backend aprende la dirección (2026-09-26). */
    val precision: Float? = null,
)

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
    /** Igual que en RecepcionRequest, para la foto/firma de la entrega. */
    val archivoPerdido: Boolean = false,
    /** Error del GPS en metros: con buena precisión el backend aprende la dirección (2026-09-26). */
    val precision: Float? = null,
)

/** Nota de texto libre sobre el viaje (ej. "entregado en porteria a Fulano") — se ve en el detalle del panel. */
data class ComentarioRequest(val texto: String)

/** "Reportar al cliente" (spec-antiabuso Fase 3). tipo: DEMORO | NO_DECLARO_VALORES | PEDIDO_FALSO | OTRO. */
data class ReporteClienteRequest(val tipo: String, val nota: String?)

/** Sección "Finalizados" de la app — espeja PedidoDtos.HistorialResponse del backend. */
data class HistorialDto(
    val pedidos: List<PedidoDto>,
    val cantidadViajes: Int,
    val montoTotal: Double,
    val cantidadRechazados: Long,
    val cantidadNoAceptados: Long,
)

/** Espeja CadeteController.ConectadoHoyResponse. */
data class ConectadoHoyDto(val minutos: Long)

/** Cuerpo de los mensajes que llegan por /queue/cadete/{id}/viajes (WebSocketPublisher.EventoViaje). */
data class EventoViajeDto(
    val tipo: String,
    val pedido: PedidoDto,
)

object EventoViaje {
    const val VIAJE_ASIGNADO = "VIAJE_ASIGNADO"
    const val VIAJE_QUITADO = "VIAJE_QUITADO"
    const val VIAJE_CANCELADO = "VIAJE_CANCELADO"
    /** El cliente reclamó desde el seguimiento: solo hay que recargar el viaje para mostrarlo. */
    const val RECLAMO_CLIENTE = "RECLAMO_CLIENTE"
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
