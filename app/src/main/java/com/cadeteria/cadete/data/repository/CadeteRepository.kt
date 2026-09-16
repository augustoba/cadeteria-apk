package com.cadeteria.cadete.data.repository

import com.cadeteria.cadete.data.remote.RetrofitProvider
import com.cadeteria.cadete.data.remote.dto.CadeteConfigDto
import com.cadeteria.cadete.data.remote.dto.CadeteDto
import com.cadeteria.cadete.data.remote.dto.CambiarPasswordRequest
import com.cadeteria.cadete.data.remote.dto.CuentaRequest
import com.cadeteria.cadete.data.remote.dto.EstadoRequest
import com.cadeteria.cadete.data.remote.dto.FcmTokenRequest
import com.cadeteria.cadete.data.remote.dto.MiSemanaDto
import com.cadeteria.cadete.data.remote.dto.TelefonoRequest
import com.cadeteria.cadete.data.remote.dto.UbicacionRequest

class CadeteRepository(private val retrofitProvider: RetrofitProvider) {

    suspend fun miPerfil(): Result<CadeteDto> = runCatching { retrofitProvider.apiService().miPerfil() }

    suspend fun miConfiguracion(): Result<CadeteConfigDto> =
        runCatching { retrofitProvider.apiService().miConfiguracion() }

    suspend fun actualizarEstado(estadoId: String): Result<CadeteDto> =
        runCatching { retrofitProvider.apiService().actualizarEstado(EstadoRequest(estadoId)) }

    suspend fun actualizarUbicacion(lat: Double, lng: Double): Result<CadeteDto> =
        runCatching { retrofitProvider.apiService().actualizarUbicacion(UbicacionRequest(lat, lng)) }

    suspend fun actualizarFcmToken(token: String): Result<CadeteDto> =
        runCatching { retrofitProvider.apiService().actualizarFcmToken(FcmTokenRequest(token)) }

    suspend fun cambiarPassword(actual: String, nueva: String): Result<CadeteDto> =
        runCatching { retrofitProvider.apiService().cambiarPassword(CambiarPasswordRequest(actual, nueva)) }

    suspend fun actualizarTelefono(telefono: String): Result<CadeteDto> =
        runCatching { retrofitProvider.apiService().actualizarTelefono(TelefonoRequest(telefono)) }

    suspend fun actualizarCuenta(cbu: String?, aliasCbu: String?): Result<CadeteDto> =
        runCatching { retrofitProvider.apiService().actualizarCuenta(CuentaRequest(cbu, aliasCbu)) }

    suspend fun miPagoSemanal(): Result<MiSemanaDto> = runCatching { retrofitProvider.apiService().miPagoSemanal() }

    suspend fun marcarAvisoLeido(avisoId: String): Result<Unit> =
        runCatching { retrofitProvider.apiService().marcarAvisoLeido(avisoId) }

    /** Avisos generales que todavía no vio (ronda 10, punto 98). */
    suspend fun avisosPendientes(): Result<List<com.cadeteria.cadete.data.remote.dto.AvisoGeneralDto>> =
        runCatching { retrofitProvider.apiService().avisosPendientes() }

    /** Pantalla "Avisos" con historial, ya leídos incluidos (mejora 2026-09-16). */
    suspend fun historialAvisos(): Result<List<com.cadeteria.cadete.data.remote.dto.AvisoHistorialDto>> =
        runCatching { retrofitProvider.apiService().historialAvisos() }
}
