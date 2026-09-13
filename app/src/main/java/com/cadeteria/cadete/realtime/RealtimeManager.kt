package com.cadeteria.cadete.realtime

import com.cadeteria.cadete.data.local.SessionManager
import com.cadeteria.cadete.data.remote.dto.AvisoDto
import com.cadeteria.cadete.data.remote.dto.EventoViajeDto
import com.cadeteria.cadete.data.remote.dto.MensajeDto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Une el StompClient con la sesión: arma la URL de WS a partir de la URL http del
 * backend, se suscribe a los eventos de viaje y chat del cadete logueado, y
 * reconecta solo con backoff simple si se corta (Wi-Fi del local, datos móviles
 * inestables del cadete en la calle, etc — diseno-tecnico.md sección 4).
 */
class RealtimeManager(private val session: SessionManager) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val gson = Gson()
    private var client: StompClient? = null
    private var reconectando = false

    private val _eventosViaje = MutableSharedFlow<EventoViajeDto>(extraBufferCapacity = 8)
    val eventosViaje: SharedFlow<EventoViajeDto> = _eventosViaje.asSharedFlow()

    private val _mensajesChat = MutableSharedFlow<MensajeDto>(extraBufferCapacity = 16)
    val mensajesChat: SharedFlow<MensajeDto> = _mensajesChat.asSharedFlow()

    /** Avisos generales del admin o recordatorios de demora (PedidoService.avisosDemora en el backend). */
    private val _avisos = MutableSharedFlow<AvisoDto>(extraBufferCapacity = 8)
    val avisos: SharedFlow<AvisoDto> = _avisos.asSharedFlow()

    private val _conectado = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1)
    val conectado: SharedFlow<Boolean> = _conectado.asSharedFlow()

    fun start() {
        scope.launch {
            val cadeteId = session.cadeteIdSync()
            if (cadeteId.isNullOrBlank()) return@launch
            conectar(cadeteId)
        }
    }

    fun stop() {
        reconectando = false
        client?.disconnect()
        client = null
    }

    private suspend fun conectar(cadeteId: String) {
        val baseHttp = session.baseUrlSync()
        val wsUrl = baseHttp
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/ws"

        val nuevo = StompClient(wsUrl) { session.tokenSync() }
        nuevo.onConnected = {
            _conectado.tryEmit(true)
            nuevo.subscribe("/queue/cadete/$cadeteId/viajes") { body ->
                runCatching { gson.fromJson(body, EventoViajeDto::class.java) }
                    .onSuccess { _eventosViaje.tryEmit(it) }
            }
            nuevo.subscribe("/queue/cadete/$cadeteId/chat") { body ->
                runCatching { gson.fromJson(body, MensajeDto::class.java) }
                    .onSuccess { _mensajesChat.tryEmit(it) }
            }
            nuevo.subscribe("/queue/cadete/$cadeteId/avisos") { body ->
                runCatching { gson.fromJson(body, AvisoDto::class.java) }
                    .onSuccess { _avisos.tryEmit(it) }
            }
        }
        nuevo.onDisconnected = {
            _conectado.tryEmit(false)
            if (!reconectando) {
                reconectando = true
                scope.launch {
                    delay(5_000)
                    reconectando = false
                    conectar(cadeteId)
                }
            }
        }
        client = nuevo
        nuevo.connect()
    }
}
