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
import com.cadeteria.cadete.data.remote.mensajeDelServidor
import com.cadeteria.cadete.push.NotificationHelper
import com.cadeteria.cadete.widget.CadeteWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val cargando: Boolean = true,
    val cadete: CadeteDto? = null,
    /** La app instalada es más vieja que la que exige la cadetería: no puede activarse hasta actualizar (2026-10-03). */
    val versionVieja: Boolean = false,
    /** Cartel "Hay una versión nueva": al abrir la app y cada vez que quiere activarse con la vieja. */
    val avisoVersionVieja: Boolean = false,
    val pidiendoLinkDescarga: Boolean = false,
    /** Sección "Asignados y en curso" — puede haber más de uno si el tope de viajes lo permite. */
    val activos: List<PedidoDto> = emptyList(),
    /** Viajes con Retirado/parada/Entregado guardado sin señal: la tarjeta lo avisa (2026-09-29). */
    val encolados: Set<String> = emptySet(),
    /** Incidente por reclamo de un cliente: mientras esté abierto no le llegan pedidos (2026-09-26). */
    val incidenteAbierto: com.cadeteria.cadete.data.remote.dto.IncidenteAbiertoDto? = null,
    val error: String? = null,
    val cambiandoEstado: Boolean = false,
    /** Popup "Bienvenido {nombre}, tenés $X de saldo" al entrar — solo cadetes PORCENTAJE. */
    val bienvenida: BienvenidaInfo? = null,
    /** Cartel "Antes de arrancar" al entrar (textos de Configuración, 2026-09-29); null = no se muestra. */
    val recordatorios: RecordatoriosCartel? = null,
    /**
     * Avisos generales del admin sin "Entendido" todavía, del más viejo al más nuevo (2026-09-29): se
     * muestran de a uno en un cartel que no se cierra solo. "Entendido" es lo que los marca leídos.
     */
    val avisosGenerales: List<com.cadeteria.cadete.data.remote.dto.AvisoGeneralDto> = emptyList(),
    /** Recordatorio de demora del sistema (no trae avisoId): banner arriba que se cierra solo. */
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
    /** "Avisos de la calle" activos cerca (carril C, 2026-09-28): los que llegan en vivo y los pedidos al abrir. */
    val avisosCalle: List<com.cadeteria.cadete.data.remote.dto.AvisoCalleDto> = emptyList(),
    val enviandoAvisoCalle: Boolean = false,
    /** Confirmación ("Avisaste: Control en …") o error del botón "Avisar" — se muestra y se cierra. */
    val mensajeAvisoCalle: String? = null,
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
        escucharAvisosCalle()
        cargarAvisosCalleCerca()
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
                // Antes la versión solo se miraba al iniciar sesión: con la sesión abierta se seguía con la vieja.
                val vieja = com.cadeteria.cadete.BuildConfig.VERSION_CODE < it.versionMinimaApp
                _uiState.value = _uiState.value.copy(
                    tiempoLimiteAceptacionSeg = it.tiempoLimiteAceptacionSeg,
                    checklistDocumentacionObligatorio = it.checklistDocumentacionObligatorio,
                    versionVieja = vieja,
                    avisoVersionVieja = vieja && !_uiState.value.versionVieja,
                )
            }
        }
    }

    fun cerrarAvisoVersionVieja() {
        _uiState.value = _uiState.value.copy(avisoVersionVieja = false)
    }

    /** El link es de un solo uso: se pide recién al tocar "Descargar". Null si no se pudo conseguir. */
    fun pedirLinkDescarga(onLink: (String?) -> Unit) {
        if (_uiState.value.pidiendoLinkDescarga) return
        _uiState.value = _uiState.value.copy(pidiendoLinkDescarga = true)
        viewModelScope.launch {
            val link = app.cadeteRepository.linkApk().getOrNull()
            _uiState.value = _uiState.value.copy(pidiendoLinkDescarga = false)
            onLink(link)
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
            // Sin notificación: el cadete ya está mirando la app, le sale el cartel (2026-09-29).
            val pendientes = app.cadeteRepository.avisosPendientes().getOrNull() ?: return@launch
            _uiState.value = _uiState.value.copy(
                avisosGenerales = CartelesInicio.encolar(_uiState.value.avisosGenerales, pendientes),
            )
        }
    }

    /**
     * "Entendido" del primer aviso de la cola: recién ahí queda leído (2026-09-29; antes se marcaba al
     * llegar y el panel contaba como vistos avisos que nadie leyó). Si no hay señal, vuelve a salir la
     * próxima vez que abra la app: mejor repetido que perdido.
     */
    fun entenderAvisoGeneral() {
        val aviso = _uiState.value.avisosGenerales.firstOrNull() ?: return
        _uiState.value = _uiState.value.copy(avisosGenerales = _uiState.value.avisosGenerales.drop(1))
        viewModelScope.launch { app.cadeteRepository.marcarAvisoLeido(aviso.id) }
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

    /** Una vez por login. Sin configuración (sin señal, backend viejo) salen los textos de siempre. */
    private fun cargarRecordatoriosSiCorresponde() {
        if (!app.mostrarRecordatoriosAlEntrar) return
        app.mostrarRecordatoriosAlEntrar = false
        viewModelScope.launch {
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            _uiState.value = _uiState.value.copy(recordatorios = CartelesInicio.recordatorios(config))
        }
    }

    /** "Entendido": queda en la ficha del cadete con lo que decía el cartel. Sin señal no se reintenta. */
    fun entenderRecordatorios() {
        _uiState.value = _uiState.value.copy(recordatorios = null)
        viewModelScope.launch { runCatching { app.retrofitProvider.apiService().recordatoriosEntendido() } }
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
            val incidente = app.cadeteRepository.incidenteAbierto().getOrNull()
            val encolados = app.pendingActionsRepository.pedidosConEncolados()
            _uiState.value = _uiState.value.copy(
                incidenteAbierto = incidente,
                encolados = encolados,
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
    fun toggleDisponibilidad(onLocationServiceStart: () -> Unit) {
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
        cambiarEstado(nuevoEstado, onLocationServiceStart)
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
    fun toggleOcupado(onLocationServiceStart: () -> Unit) {
        val actual = _uiState.value.cadete ?: return
        if (actual.estado.id == EstadoCadete.DESCONECTADO) return
        val nuevoEstado = if (actual.estado.id == EstadoCadete.OCUPADO) EstadoCadete.LIBRE else EstadoCadete.OCUPADO
        cambiarEstado(nuevoEstado, onLocationServiceStart)
    }

    private fun cambiarEstado(nuevoEstado: String, onLocationServiceStart: () -> Unit) {
        // Con la app vieja no se puede activar (el servidor tampoco lo deja); desconectarse, sí.
        if (_uiState.value.versionVieja && nuevoEstado != EstadoCadete.DESCONECTADO) {
            _uiState.value = _uiState.value.copy(avisoVersionVieja = true)
            return
        }
        _uiState.value = _uiState.value.copy(cambiandoEstado = true)
        viewModelScope.launch {
            app.cadeteRepository.actualizarEstado(nuevoEstado)
                .onSuccess { actualizado ->
                    _uiState.value = _uiState.value.copy(cadete = actualizado, cambiandoEstado = false)
                    // En cualquier estado, también Desconectado (2026-09-29, pedido del usuario): la ubicación
                    // se manda mientras haya sesión abierta y sirve para aprender calles. Corta solo "Salir".
                    onLocationServiceStart()
                    CadeteWidget.sincronizarEstado(app, actualizado.estado.id, actualizado.nombre)
                    app.recordatorioEstado.actualizar(actualizado.estado.id)
                    // Al pasar a Libre, los avisos de la calle que llegaron mientras estaba desconectado.
                    if (nuevoEstado == EstadoCadete.LIBRE) cargarAvisosCalleCerca()
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

    /**
     * Botón "🚨 Avisar" (carril C, 2026-09-28): un toque, sin escribir nada. La ubicación y la hora
     * se toman solas. Sin señal no se encola: un aviso de hace 20 min ya no sirve.
     */
    fun avisarCalle(tipo: String) {
        if (_uiState.value.enviandoAvisoCalle) return
        _uiState.value = _uiState.value.copy(enviandoAvisoCalle = true, mensajeAvisoCalle = null)
        viewModelScope.launch {
            val ubicacion = com.cadeteria.cadete.location.ubicacionParaAviso(app)
            if (ubicacion == null) {
                terminarAvisoCalle("No pudimos leer tu ubicación. Revisá que el GPS esté prendido.")
                return@launch
            }
            if (ubicacion.simulada) {
                terminarAvisoCalle("Tu celular está usando una ubicación simulada. Desactivá esa app para avisar.")
                return@launch
            }
            // La calle del Geocoder del teléfono (2026-09-29): la misma que ve el panel. Sin ella el backend
            // usa OpenStreetMap, que en algunas zonas nombra otra calle (Colombia 4695 → "Camino del Perú").
            val calle = ubicacion.calle ?: com.cadeteria.cadete.location.calleDelTelefono(app, ubicacion.lat, ubicacion.lng)
            runCatching {
                app.retrofitProvider.apiService().avisarCalle(
                    com.cadeteria.cadete.data.remote.dto.AvisoCalleRequest(
                        tipo, ubicacion.lat, ubicacion.lng, ubicacion.precisionM, calle?.calle, calle?.altura,
                    ),
                )
            }
                .onSuccess {
                    // Se guarda como propio: no se le pregunta "¿Sigue ahí?" por su propio aviso.
                    app.avisosCalleStore.marcarMio(it)
                    terminarAvisoCalle(AvisosCalleTexto.confirmacion(it))
                }
                .onFailure { e ->
                    terminarAvisoCalle(
                        if (e is java.io.IOException) "No se pudo mandar, no hay conexión."
                        else e.mensajeDelServidor() ?: "No se pudo mandar el aviso.",
                    )
                }
        }
    }

    private fun terminarAvisoCalle(mensaje: String) {
        _uiState.value = _uiState.value.copy(enviandoAvisoCalle = false, mensajeAvisoCalle = mensaje)
    }

    fun cerrarMensajeAvisoCalle() {
        _uiState.value = _uiState.value.copy(mensajeAvisoCalle = null)
    }

    /** "Avisos cerca tuyo" al abrir la app o al pasar a Libre: los activos a menos de 1 km. */
    private fun cargarAvisosCalleCerca() {
        viewModelScope.launch {
            val ubicacion = com.cadeteria.cadete.location.ubicacionActual(app) ?: return@launch
            runCatching { app.retrofitProvider.apiService().avisosCalleCerca(ubicacion.first, ubicacion.second) }
                .onSuccess { lista -> app.avisosCalleStore.reemplazar(lista) }
        }
    }

    /**
     * La lista "Avisos cerca tuyo" sale del almacén de la app (2026-09-29): ahí llegan los avisos en
     * vivo (y se notifican, ver CadeteApp) y sus cambios por "¿Sigue ahí?".
     */
    private fun escucharAvisosCalle() {
        viewModelScope.launch {
            app.avisosCalleStore.avisos.collect { lista -> _uiState.value = _uiState.value.copy(avisosCalle = lista) }
        }
    }

    /** Aviso general del admin o recordatorio de demora — siempre como notificación (vibra/suena), no solo un banner. */
    private fun escucharAvisos() {
        viewModelScope.launch {
            app.realtimeManager.avisos.collect { aviso ->
                NotificationHelper.mostrar(app, NotificationHelper.CANAL_VIAJES, aviso.mensaje.hashCode(), "Cadetería", aviso.mensaje)
                // Aviso general del admin: al cartel con "Entendido", que es lo que lo marca leído.
                if (aviso.avisoId != null) {
                    val nuevo = com.cadeteria.cadete.data.remote.dto.AvisoGeneralDto(aviso.avisoId, aviso.mensaje)
                    _uiState.value = _uiState.value.copy(
                        avisosGenerales = CartelesInicio.encolar(_uiState.value.avisosGenerales, listOf(nuevo)),
                    )
                    return@collect
                }
                _uiState.value = _uiState.value.copy(avisoFlotante = aviso.mensaje)
                // A disco, no solo en memoria: esto puede llegar con la app minimizada, y si el
                // proceso muere antes de que el cadete vuelva a verla, el banner tiene que poder
                // reconstruirse en el próximo arranque (ver cargarAvisoFlotantePendiente).
                app.sessionManager.guardarAvisoFlotantePendiente(aviso.mensaje)
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
