package com.cadeteria.cadete.data.remote.dto

/** Espeja CadeteDtos.AvisoGeneralResponse — avisos generales que este cadete todavía no vio (ronda 10, punto 98). */
data class AvisoGeneralDto(val id: String, val mensaje: String)

/** Espeja CadeteDtos.AvisoGeneralHistorialResponse — pantalla "Avisos" con historial (mejora 2026-09-16). */
data class AvisoHistorialDto(val id: String, val mensaje: String, val enviadoEn: String, val leidoPorMi: Boolean)

/** Espeja CadeteDtos.CadeteResponse del backend (GET /api/cadetes/me). */
data class CadeteDto(
    val id: String,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val telefono: String,
    val fotoUrl: String?,
    val tipoVehiculo: LookupDto,
    val vehiculoColor: String?,
    val vehiculoPatente: String?,
    val vehiculoMarca: String?,
    val vehiculoModelo: String?,
    val fotoVehiculoUrl: String?,
    val fotoCarnetUrl: String?,
    val fotoTarjetaVerdeUrl: String?,
    val fotoTarjetaVerdeDorsoUrl: String?,
    val vehiculoAnio: Int?,
    val username: String,
    val activo: Boolean,
    val estado: LookupDto,
    val lat: Double?,
    val lng: Double?,
    val ubicacionActualizadaEn: String?,
    val zonaActual: LookupDto?,
    val montoMaximoTransportado: Double?,
    val maxViajesSimultaneos: Int?,
    val ordenColaEspera: String?,
    val cbu: String?,
    val aliasCbu: String?,
    /** Calificación histórica del cadete (todo el registro) — null si todavía no lo calificó nadie. */
    val calificacionPromedio: Double?,
    val calificacionCantidad: Long,
    /** Modelo de cobro (ronda 7): "SEMANAL" o "PORCENTAJE". */
    val modalidadPago: String,
    /** Solo aplica con modalidadPago="SEMANAL" — si puede loguearse/recibir viajes esta semana. */
    val habilitadoPago: Boolean,
    val pagoSemanalMontoPagado: Double?,
    val pagoSemanalVenceEn: String?,
    /** Solo aplica con modalidadPago="PORCENTAJE" — saldo a favor, se descuenta la comisión de cada viaje. */
    val creditoDisponible: Double,
)

data class EstadoRequest(val estadoId: String)

data class UbicacionRequest(val lat: Double, val lng: Double)

data class FcmTokenRequest(val fcmToken: String)

data class CambiarPasswordRequest(val actual: String, val nueva: String)

data class TelefonoRequest(val telefono: String)

data class CuentaRequest(val cbu: String?, val aliasCbu: String?)

/** Espeja ConfiguracionDtos.CadeteConfigResponse (GET /api/cadetes/me/configuracion). */
data class CadeteConfigDto(
    val frecuenciaUbicacionSeg: Int,
    val cloudinaryCloudName: String,
    val cloudinaryUploadPreset: String,
    val versionMinimaApp: Int,
    val firmaReceptorObligatoria: Boolean,
    /** Modelo de cobro (ronda 7) — para calcular "cuánto falta" y disparar alertas locales. */
    val pagoSemanalMonto: Double,
    val comisionPorcentaje: Double,
    val creditoBajoAlertaUmbral: Double,
    /** Para la cuenta regresiva real al ofrecer un viaje nuevo (auditoría UX 2026-09-13). */
    val tiempoLimiteAceptacionSeg: Int = 120,
    /** Para la pantalla de Ayuda (mejora 2026-09-16) — vacío = esa pantalla no muestra botón de llamar. */
    val telefonoSoporte: String = "",
    /** Si hace falta carnet + tarjeta verde + foto del vehículo cargados para poder activarse (mejora 2026-09-16). */
    val checklistDocumentacionObligatorio: Boolean = false,
    /** Fotos configurables desde el panel (spec mejoras visuales §6) — se piden antes de intentar. */
    val fotoRetiroObligatoria: Boolean = false,
    val fotoEntregaObligatoria: Boolean = true,
    /** "https://.../seguimiento/" — más el token del pedido es el link del QR para el cliente. */
    val urlSeguimientoBase: String = "",
)

/** Espeja PagoSemanalDtos.MiSemanaResponse (GET /api/cadetes/me/pago-semanal). */
data class MiSemanaDto(
    val semanaInicio: String,
    val facturado: Double,
    val viajesFinalizados: Int,
    val pagado: Boolean,
)

object EstadoCadete {
    const val LIBRE = "LIBRE"
    const val OCUPADO = "OCUPADO"
    const val DESCONECTADO = "DESCONECTADO"
}

/** Espeja CadeteActualizacionDtos.ActualizacionCadeteRequest (POST /api/cadetes/me/actualizaciones). */
data class ActualizacionCadeteRequestDto(
    val fotoUrl: String? = null,
    val fotoVehiculoUrl: String? = null,
    val fotoTarjetaVerdeUrl: String? = null,
    val fotoTarjetaVerdeDorsoUrl: String? = null,
    val vehiculoMarca: String? = null,
    val vehiculoModelo: String? = null,
    val vehiculoColor: String? = null,
    val vehiculoPatente: String? = null,
    val vehiculoAnio: Int? = null,
)

/** Espeja CadeteActualizacionDtos.CampoResponse. */
data class CadeteActualizacionCampoDto(
    val id: String,
    val campo: String,
    val valorAnterior: String?,
    val valorPropuesto: String,
    val estado: String,
    val motivoRechazo: String?,
    val resueltoEn: String?,
    val resueltoPorUsername: String?,
)

/** Espeja CadeteActualizacionDtos.ActualizacionResponse (GET/POST /api/cadetes/me/actualizaciones). */
data class CadeteActualizacionDto(
    val id: String,
    val creadoEn: String,
    val estado: String,
    val campos: List<CadeteActualizacionCampoDto>,
)
