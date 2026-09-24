package com.cadeteria.cadete.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.AutorMensaje
import com.cadeteria.cadete.data.remote.dto.CadeteDto
import com.cadeteria.cadete.data.remote.dto.EstadoCadete
import com.cadeteria.cadete.data.remote.dto.EstadoPedido
import com.cadeteria.cadete.data.remote.dto.EventoViaje
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.push.NotificationHelper
import com.cadeteria.cadete.widget.CadeteWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val cargando: Boolean = true,
    val cadete: CadeteDto? = null,
    /** Sección "Asignados y en curso" — puede haber más de uno si el tope de viajes lo permite. */
    val activos: List<PedidoDto> = emptyList(),
    val error: String? = null,
    val cambiandoEstado: Boolean = false,
    /** Popup "Bienvenido {nombre}, tenés $X de saldo" al entrar — solo cadetes PORCENTAJE. */
    val bienvenida: BienvenidaInfo? = null,
    /** Popup de recordatorios (documentación, seguridad, marcar retirado/finalizado) al entrar. */
    val mostrarRecordatorios: Boolean = false,
    /** Aviso general del admin, como banner flotante arriba (además de la notificación del
     * sistema, que desaparece sola y no deja el mensaje visible en ningún lado). */
    val avisoFlotante: String? = null,
    /** Para la cuenta regresiva del viaje PENDIENTE en la tarjeta de "Asignados y en curso". */
    val tiempoLimiteAceptacionSeg: Int = 120,
    /** Checklist de documentación obligatoria antes de activarse (mejora 2026-09-16). */
    val checklistDocumentacionObligatorio: Boolean = false,
    /** Si no está vacía, se muestra el diálogo de "te falta cargar esto antes de activarte". */
    val documentacionFaltante: List<String> = emptyList(),
    /** Estadísticas de Inicio (spec mejoras visuales §2) — null mientras no cargaron o si fallaron. */
    val viajesHoy: Int? = null,
    val facturadoHoy: Double? = null,
    val minutosConectadoHoy: Long? = null,
)

data class BienvenidaInfo(val nombre: String, val saldo: Double, val saldoBajo: Boolean)

class HomeViewModel(private val app: CadeteApp) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        cargar()
        escucharEventosDeViaje()
        escucharAvisos()
        escucharMensajesChat()
        cargarAvisoFlotantePendiente()
        cargarAvisosPendientes()
        cargarBienvenidaSiCorresponde()
        cargarRecordatoriosSiCorresponde()
        cargarTiempoLimiteAceptacion()
        app.realtimeManager.start()
    }

    private fun cargarTiempoLimiteAceptacion() {
        viewModelScope.launch {
            app.cadeteRepository.miConfiguracion().onSuccess {
                _uiState.value = _uiState.value.copy(
                    tiempoLimiteAceptacionSeg = it.tiempoLimiteAceptacionSeg,
                    checklistDocumentacionObligatorio = it.checklistDocumentacionObligatorio,
                )
            }
        }
    }

    /**
     * Si el proceso murió después de mostrar la notificación de un aviso pero antes de que
     * el cadete llegara a ver el banner flotante (típico: app minimizada, Android mata el
     * proceso, el cadete recién abre la app tocando la notificación), esto lo recupera de
     * disco — no depende de que el ViewModel/WebSocket hayan seguido vivos.
     */
    private fun cargarAvisoFlotantePendiente() {
        viewModelScope.launch {
            app.sessionManager.avisoFlotantePendienteSync()?.let { mensaje ->
                _uiState.value = _uiState.value.copy(avisoFlotante = mensaje)
            }
        }
    }

    /**
     * Avisos generales que llegaron mientras la app estaba cerrada/desconectada (ronda
     * 10, punto 98) — antes solo se veían los que llegaban por WebSocket en vivo, uno
     * DESCONECTADO se los perdía para siempre. Se pide una sola vez al abrir, no en cada
     * `cargar()` (que se repite en cada evento de viaje), para no repetir notificaciones.
     */
    private fun cargarAvisosPendientes() {
        viewModelScope.launch {
            app.cadeteRepository.avisosPendientes().getOrNull()?.forEach { aviso ->
                NotificationHelper.mostrar(app, NotificationHelper.CANAL_VIAJES, aviso.mensaje.hashCode(), "Cadetería", aviso.mensaje)
                app.cadeteRepository.marcarAvisoLeido(aviso.id)
                _uiState.value = _uiState.value.copy(avisoFlotante = aviso.mensaje)
                app.sessionManager.guardarAvisoFlotantePendiente(aviso.mensaje)
            }
        }
    }

    /** Bienvenida con saldo (a pedido del dueño) — solo para PORCENTAJE, que es el único modelo donde el saldo importa para poder laburar. */
    private fun cargarBienvenidaSiCorresponde() {
        if (!app.mostrarBienvenidaAlEntrar) return
        app.mostrarBienvenidaAlEntrar = false
        viewModelScope.launch {
            val perfil = app.cadeteRepository.miPerfil().getOrNull() ?: return@launch
            if (perfil.modalidadPago != "PORCENTAJE") return@launch
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            val umbral = config?.creditoBajoAlertaUmbral ?: 500.0
            _uiState.value = _uiState.value.copy(
                bienvenida = BienvenidaInfo(perfil.nombre, perfil.creditoDisponible, perfil.creditoDisponible < umbral),
            )
        }
    }

    fun cerrarBienvenida() {
        _uiState.value = _uiState.value.copy(bienvenida = null)
    }

    private fun cargarRecordatoriosSiCorresponde() {
        if (!app.mostrarRecordatoriosAlEntrar) return
        app.mostrarRecordatoriosAlEntrar = false
        _uiState.value = _uiState.value.copy(mostrarRecordatorios = true)
    }

    fun cerrarRecordatorios() {
        _uiState.value = _uiState.value.copy(mostrarRecordatorios = false)
    }

    fun cerrarAvisoFlotante() {
        _uiState.value = _uiState.value.copy(avisoFlotante = null)
        viewModelScope.launch { app.sessionManager.limpiarAvisoFlotantePendiente() }
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            val perfil = app.cadeteRepository.miPerfil()
            val activos = app.pedidoRepository.viajesActivos()
            _uiState.value = _uiState.value.copy(
                cargando = false,
                cadete = perfil.getOrNull(),
                // El asignado sin aceptar (PENDIENTE) va primero — tiene tiempo límite para
                // responder, no debería quedar abajo de la lista si ya hay otros en curso.
                activos = activos.getOrDefault(emptyList())
                    .sortedByDescending { it.estado.id == EstadoPedido.PENDIENTE },
                error = if (perfil.isFailure) "No se pudo cargar tu perfil." else null,
            )
            perfil.getOrNull()?.let {
                CadeteWidget.sincronizarEstado(app, it.estado.id, it.nombre)
                app.recordatorioEstado.actualizar(it.estado.id)
            }
            cargarEstadisticasDeHoy()
        }
    }

    /** Viajes hoy / Facturado / Conectado. Si falla no muestra error: son un extra, no bloquean nada. */
    private suspend fun cargarEstadisticasDeHoy() {
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("America/Argentina/Buenos_Aires")
        }.format(java.util.Date())
        val resumen = app.pedidoRepository.resumenDelDia(hoy).getOrNull()
        val minutos = app.pedidoRepository.minutosConectadoHoy().getOrNull()
        _uiState.value = _uiState.value.copy(
            viajesHoy = resumen?.cantidadViajes,
            facturadoHoy = resumen?.montoTotal,
            minutosConectadoHoy = minutos,
        )
    }

    /**
     * El botón grande "Activarme"/"Desconectarme" — ahora funciona también estando OCUPADO
     * (a pedido del dueño: antes quedaba trabado hasta terminar todos los viajes), PERO
     * desconectarse (no activarse) sigue bloqueado si todavía tiene viajes PENDIENTE o
     * EN_CURSO — mismo criterio que ya usa "Cerrar sesión" del menú lateral, para no perder
     * el seguimiento de un viaje en curso.
     */
    fun toggleDisponibilidad(onLocationServiceStart: () -> Unit, onLocationServiceStop: () -> Unit) {
        val actual = _uiState.value.cadete ?: return
        val nuevoEstado = if (actual.estado.id == EstadoCadete.DESCONECTADO) EstadoCadete.LIBRE else EstadoCadete.DESCONECTADO
        if (nuevoEstado == EstadoCadete.DESCONECTADO && _uiState.value.activos.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "No te podés desconectar con viajes pendientes o en curso — finalizalos primero.",
            )
            return
        }
        if (nuevoEstado == EstadoCadete.LIBRE && _uiState.value.checklistDocumentacionObligatorio) {
            val faltantes = documentacionFaltante(actual)
            if (faltantes.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(documentacionFaltante = faltantes)
                return
            }
        }
        cambiarEstado(nuevoEstado, onLocationServiceStart, onLocationServiceStop)
    }

    /** Checklist de documentación obligatoria antes de activarse (mejora 2026-09-16) — reviso a ojo lo mismo que ya valida el backend, para avisar antes de intentar y no solo mostrar el error genérico del 400. */
    private fun documentacionFaltante(c: com.cadeteria.cadete.data.remote.dto.CadeteDto): List<String> {
        val faltantes = mutableListOf<String>()
        if (c.fotoCarnetUrl.isNullOrBlank()) faltantes.add("Carnet de conducir")
        if (c.fotoTarjetaVerdeUrl.isNullOrBlank()) faltantes.add("Tarjeta verde")
        if (c.fotoVehiculoUrl.isNullOrBlank()) faltantes.add("Foto del vehículo")
        return faltantes
    }

    fun cerrarDialogoDocumentacion() {
        _uiState.value = _uiState.value.copy(documentacionFaltante = emptyList())
    }

    /**
     * "Ponerme Ocupado" (a pedido del dueño): el cadete puede frenar que le sigan asignando
     * viajes aunque todavía tenga lugar según su tope de viajes simultáneos — antes esto
     * solo lo decidía el sistema en automático y no se podía tocar a mano.
     */
    fun toggleOcupado(onLocationServiceStart: () -> Unit, onLocationServiceStop: () -> Unit) {
        val actual = _uiState.value.cadete ?: return
        if (actual.estado.id == EstadoCadete.DESCONECTADO) return
        val nuevoEstado = if (actual.estado.id == EstadoCadete.OCUPADO) EstadoCadete.LIBRE else EstadoCadete.OCUPADO
        cambiarEstado(nuevoEstado, onLocationServiceStart, onLocationServiceStop)
    }

    private fun cambiarEstado(nuevoEstado: String, onLocationServiceStart: () -> Unit, onLocationServiceStop: () -> Unit) {
        _uiState.value = _uiState.value.copy(cambiandoEstado = true)
        viewModelScope.launch {
            app.cadeteRepository.actualizarEstado(nuevoEstado)
                .onSuccess { actualizado ->
                    _uiState.value = _uiState.value.copy(cadete = actualizado, cambiandoEstado = false)
                    if (nuevoEstado == EstadoCadete.DESCONECTADO) onLocationServiceStop() else onLocationServiceStart()
                    CadeteWidget.sincronizarEstado(app, actualizado.estado.id, actualizado.nombre)
                    app.recordatorioEstado.actualizar(actualizado.estado.id)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        cambiandoEstado = false,
                        error = "No se pudo cambiar tu estado.",
                    )
                }
        }
    }

    private fun escucharEventosDeViaje() {
        viewModelScope.launch {
            app.realtimeManager.eventosViaje.collect { evento ->
                // Cualquier evento (asignado/quitado/cancelado) puede cambiar la lista y el
                // estado (LIBRE/OCUPADO) — más simple y confiable recargar todo que parchear.
                cargar()
                // Con la app abierta este evento llega por WebSocket, no por FCM (ver
                // CadeteFirebaseMessagingService) — sin este aviso, un viaje nuevo entraba
                // en silencio y se podía pasar el tiempo límite para aceptarlo sin notarlo.
                // Mismo problema con QUITADO/CANCELADO (mejora 2026-09-23, pedida por el
                // dueño): antes solo se recargaba la lista y el pedido desaparecía de la
                // pantalla sin ningún aviso — el cadete nunca se enteraba de qué había pasado.
                when (evento.tipo) {
                    EventoViaje.VIAJE_ASIGNADO -> NotificationHelper.mostrar(
                        app, NotificationHelper.CANAL_VIAJES, evento.pedido.id.hashCode(),
                        "Nuevo viaje", "Tenés asignado el pedido #${evento.pedido.numero}.",
                        destino = NotificationHelper.DESTINO_VIAJE, pedidoId = evento.pedido.id,
                    )
                    EventoViaje.VIAJE_QUITADO -> NotificationHelper.mostrar(
                        app, NotificationHelper.CANAL_VIAJES, evento.pedido.id.hashCode(),
                        "Viaje quitado", "Se te quitó el pedido #${evento.pedido.numero}.",
                    )
                    EventoViaje.VIAJE_CANCELADO -> NotificationHelper.mostrar(
                        app, NotificationHelper.CANAL_VIAJES, evento.pedido.id.hashCode(),
                        "Viaje cancelado", "Se canceló el pedido #${evento.pedido.numero}.",
                    )
                }
            }
        }
    }

    /** Aviso general del admin o recordatorio de demora — siempre como notificación (vibra/suena), no solo un banner. */
    private fun escucharAvisos() {
        viewModelScope.launch {
            app.realtimeManager.avisos.collect { aviso ->
                NotificationHelper.mostrar(app, NotificationHelper.CANAL_VIAJES, aviso.mensaje.hashCode(), "Cadetería", aviso.mensaje)
                _uiState.value = _uiState.value.copy(avisoFlotante = aviso.mensaje)
                // A disco, no solo en memoria: esto puede llegar con la app minimizada, y si el
                // proceso muere antes de que el cadete vuelva a verla, el banner tiene que poder
                // reconstruirse en el próximo arranque (ver cargarAvisoFlotantePendiente).
                app.sessionManager.guardarAvisoFlotantePendiente(aviso.mensaje)
                // Los recordatorios de demora no traen avisoId — solo se confirma lectura de avisos generales.
                aviso.avisoId?.let { app.cadeteRepository.marcarAvisoLeido(it) }
            }
        }
    }

    /** Mensaje de chat del admin — con la app abierta llega por WebSocket, no por FCM (ver
     * CadeteFirebaseMessagingService), así que sin esto entraba en silencio igual que pasaba
     * antes con los viajes. Se ignoran los propios mensajes del cadete (eco del WebSocket). */
    private fun escucharMensajesChat() {
        viewModelScope.launch {
            app.realtimeManager.mensajesChat.collect { mensaje ->
                if (mensaje.autor != AutorMensaje.ADMIN) return@collect
                app.sumarChatNoLeido()
                val cuerpo = mensaje.texto
                    ?: if (mensaje.audioUrl != null) "Nota de voz" else if (mensaje.imagenUrl != null) "Imagen" else "Nuevo mensaje"
                NotificationHelper.mostrar(
                    app, NotificationHelper.CANAL_CHAT, mensaje.id.hashCode(), "Nuevo mensaje", cuerpo,
                    destino = NotificationHelper.DESTINO_CHAT,
                )
            }
        }
    }
}
