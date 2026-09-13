package com.cadeteria.cadete.data.repository

import com.cadeteria.cadete.data.remote.RetrofitProvider
import com.cadeteria.cadete.data.remote.dto.MensajeDto
import com.cadeteria.cadete.data.remote.dto.MensajeRequest

class ChatRepository(private val retrofitProvider: RetrofitProvider) {

    suspend fun historial(cadeteId: String): Result<List<MensajeDto>> =
        runCatching { retrofitProvider.apiService().historialChat(cadeteId) }

    suspend fun enviar(cadeteId: String, texto: String): Result<MensajeDto> =
        runCatching { retrofitProvider.apiService().enviarMensaje(cadeteId, MensajeRequest(texto, null, null)) }

    suspend fun enviarNotaDeVoz(cadeteId: String, audioUrl: String): Result<MensajeDto> =
        runCatching { retrofitProvider.apiService().enviarMensaje(cadeteId, MensajeRequest(null, audioUrl, null)) }

    suspend fun enviarImagen(cadeteId: String, imagenUrl: String): Result<MensajeDto> =
        runCatching { retrofitProvider.apiService().enviarMensaje(cadeteId, MensajeRequest(null, null, imagenUrl)) }

    suspend fun marcarLeido(cadeteId: String): Result<Unit> =
        runCatching { retrofitProvider.apiService().marcarLeido(cadeteId) }
}
