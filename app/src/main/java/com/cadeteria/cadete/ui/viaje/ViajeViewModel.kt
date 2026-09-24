package com.cadeteria.cadete.ui.viaje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.EstadoCadete
import com.cadeteria.cadete.data.remote.dto.EventoViaje
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.data.remote.dto.RutaResponseDto
import com.cadeteria.cadete.location.ubicacionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ViajeUiState(
    val cargando: Boolean = true,
    val viaje: PedidoDto? = null,
    val ruta: RutaResponseDto? = null,
    val enviando: Boolean = false,
    val error: String? = null,
    /** true cuando el viaje se cerró (finalizado/quitado/cancelado) — la pantalla se puede cerrar. */
    val terminado: Boolean = false,
    /** true cuando este era el último viaje activo del cadete — se le pregunta si sigue libre o se desactiva (a pedido del dueño). */
    val preguntarSiSigueLibre: Boolean = false,
    /** true recién aceptado un viaje — se le pregunta si quiere seguir recibiendo pedidos o ponerse OCUPADO (a pedido del dueño). */
    val preguntarSiQuedaOcupado: Boolean = false,
    /** true cuando "Finalizar" se encoló porque no había conexión — la pantalla queda mostrando el aviso
     * hasta que el cadete toca "Volver" (no cierra sola, para que el aviso no pase desapercibido). */
    val finalizarEncolado: Boolean = false,
    /** Mismo caso que finalizarEncolado pero para "Marcar como retirado" (ronda 3, punto 24). */
    val retiradoEncolado: Boolean = false,
    val enviandoComentario: Boolean = false,
    val enviandoReporte: Boolean = false,
    /** true tras guardar un reporte del cliente — la pantalla muestra la confirmación (spec-antiabuso Fase 3). */
    val reporteRegistrado: Boolean = false,
    /** Si Configuración exige la firma digital del receptor para poder finalizar (ronda 3, punto 51). */
    val firmaReceptorObligatoria: Boolean = false,
    /** Fotos configurables desde el panel (spec mejoras visuales §6). */
    val fotoRetiroObligatoria: Boolean = false,
    val fotoEntregaObligatoria: Boolean = true,
    /** Para la cuenta regresiva real al ofrecer un viaje nuevo (auditoría UX 2026-09-13). */
    val tiempoLimiteAceptacionSeg: Int = 120,
)

class ViajeViewModel(private val app: CadeteApp, private val pedidoId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(ViajeUiState())
    val uiState: StateFlow<ViajeUiState> = _uiState.asStateFlow()

    init {
        cargar()
        escucharEventos()
        viewModelScope.launch {
            app.cadeteRepository.miConfiguracion().onSuccess {
                _uiState.value = _uiState.value.copy(
                    firmaReceptorObligatoria = it.firmaReceptorObligatoria,
                    fotoRetiroObligatoria = it.fotoRetiroObligatoria,
                    fotoEntregaObligatoria = it.fotoEntregaObligatoria,
                    tiempoLimiteAceptacionSeg = it.tiempoLimiteAceptacionSeg,
                )
            }
        }
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            val res = app.pedidoRepository.detalle(pedidoId)
            val viaje = res.getOrNull()
            _uiState.value = _uiState.value.copy(cargando = false, viaje = viaje, terminado = viaje == null)
            if (viaje != null) cargarRuta(viaje.id)
        }
    }

    private fun cargarRuta(id: String) {
        viewModelScope.launch {
            app.pedidoRepository.ruta(id).onSuccess { r -> _uiState.value = _uiState.value.copy(ruta = r) }
        }
    }

    fun aceptar() {
        val id = _uiState.value.viaje?.id ?: return
        _uiState.value = _uiState.value.copy(enviando = true)
        viewModelScope.launch {
            app.pedidoRepository.aceptar(id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(viaje = it, enviando = false, preguntarSiQuedaOcupado = true)
                    cargarRuta(id)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        error = "No se pudo aceptar — puede que ya se le haya ofrecido a otro cadete.",
                    )
                }
        }
    }

    fun rechazar(motivo: String? = null) {
        val id = _uiState.value.viaje?.id ?: return
        _uiState.value = _uiState.value.copy(enviando = true)
        viewModelScope.launch {
            app.pedidoRepository.rechazar(id, motivo)
                .onSuccess { _uiState.value = _uiState.value.copy(enviando = false, viaje = null, terminado = true) }
                .onFailure { _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo rechazar el viaje.") }
        }
    }

    /**
     * Botón "Retirado" — la foto es opcional. Se adjunta la ubicación actual del cadete
     * (si hay GPS disponible) para que el admin pueda verificar en el mapa que el retiro
     * fue en la dirección real declarada por el cliente.
     * Modo offline básico (ronda 3, punto 24 — misma idea que "Finalizar"): si no hay
     * conexión, se encola y la app la reintenta sola cuando vuelva internet.
     */
    fun marcarRetirado(foto: File?) {
        val id = _uiState.value.viaje?.id ?: return
        _uiState.value = _uiState.value.copy(enviando = true, error = null)
        viewModelScope.launch {
            val ubicacion = ubicacionActual(app)
            if (foto == null) {
                ejecutarRetirado(id, fotoUrl = null, fotoPathLocal = null, ubicacion = ubicacion)
                return@launch
            }
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            if (config == null) {
                _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo leer la configuración del servidor.")
                return@launch
            }
            app.cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, foto).fold(
                onSuccess = { url -> ejecutarRetirado(id, fotoUrl = url, fotoPathLocal = null, ubicacion = ubicacion) },
                onFailure = { e ->
                    if (e is java.io.IOException) {
                        encolarRetiradoOffline(id, fotoUrl = null, fotoPathLocal = foto.absolutePath, ubicacion)
                    } else {
                        _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo subir la foto.")
                    }
                },
            )
        }
    }

    private suspend fun ejecutarRetirado(id: String, fotoUrl: String?, fotoPathLocal: String?, ubicacion: Pair<Double, Double>?) {
        app.pedidoRepository.marcarRetirado(id, fotoUrl, ubicacion?.first, ubicacion?.second)
            .onSuccess { _uiState.value = _uiState.value.copy(enviando = false, viaje = it) }
            .onFailure { e ->
                if (e is java.io.IOException) {
                    encolarRetiradoOffline(id, fotoUrl, fotoPathLocal, ubicacion)
                } else {
                    _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo marcar como retirado.")
                }
            }
    }

    private suspend fun encolarRetiradoOffline(id: String, fotoUrl: String?, fotoPathLocal: String?, ubicacion: Pair<Double, Double>?) {
        app.pendingActionsRepository.encolarRetirado(id, fotoUrl, fotoPathLocal, ubicacion?.first, ubicacion?.second)
        _uiState.value = _uiState.value.copy(enviando = false, retiradoEncolado = true)
    }

    /**
     * Misma idea que marcarRetirado: se adjunta la ubicación actual del cadete al finalizar.
     * Modo offline básico: si no hay conexión (ni para subir la foto, ni para el POST de
     * finalizar), en vez de perder el intento se encola en PendingActionsRepository — la
     * app la reintenta sola cuando vuelve internet, no hace falta que el cadete haga nada.
     */
    fun finalizar(receptorNombre: String?, foto: File?, firma: File?) {
        val id = _uiState.value.viaje?.id ?: return
        _uiState.value = _uiState.value.copy(enviando = true, error = null)
        viewModelScope.launch {
            val ubicacion = ubicacionActual(app)
            if (foto == null && firma == null) {
                ejecutarFinalizar(id, receptorNombre, null, null, null, null, ubicacion)
                return@launch
            }
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            if (config == null) {
                _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo leer la configuración del servidor.")
                return@launch
            }

            var fotoUrl: String? = null
            var fotoPathLocal: String? = null
            if (foto != null) {
                val resultado = app.cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, foto)
                if (resultado.isFailure && resultado.exceptionOrNull() !is java.io.IOException) {
                    _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo subir la foto.")
                    return@launch
                }
                fotoUrl = resultado.getOrNull()
                fotoPathLocal = if (fotoUrl == null) foto.absolutePath else null
            }

            var firmaUrl: String? = null
            var firmaPathLocal: String? = null
            if (firma != null) {
                val resultado = app.cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, firma, "image/jpeg")
                if (resultado.isFailure && resultado.exceptionOrNull() !is java.io.IOException) {
                    _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo subir la firma.")
                    return@launch
                }
                firmaUrl = resultado.getOrNull()
                firmaPathLocal = if (firmaUrl == null) firma.absolutePath else null
            }

            if (fotoPathLocal != null || firmaPathLocal != null) {
                encolarFinalizarOffline(id, receptorNombre, fotoUrl, fotoPathLocal, firmaUrl, firmaPathLocal, ubicacion)
            } else {
                ejecutarFinalizar(id, receptorNombre, fotoUrl, null, firmaUrl, null, ubicacion)
            }
        }
    }

    private suspend fun ejecutarFinalizar(
        id: String,
        receptorNombre: String?,
        fotoUrl: String?,
        fotoPathLocal: String?,
        firmaUrl: String?,
        firmaPathLocal: String?,
        ubicacion: Pair<Double, Double>?,
    ) {
        app.pedidoRepository.finalizar(id, receptorNombre, fotoUrl, firmaUrl, ubicacion?.first, ubicacion?.second)
            .onSuccess {
                val quedanActivos = app.pedidoRepository.viajesActivos().getOrNull()?.isNotEmpty() ?: true
                _uiState.value = _uiState.value.copy(
                    enviando = false,
                    viaje = it,
                    terminado = true,
                    preguntarSiSigueLibre = !quedanActivos,
                )
            }
            .onFailure { e ->
                if (e is java.io.IOException) {
                    encolarFinalizarOffline(id, receptorNombre, fotoUrl, fotoPathLocal, firmaUrl, firmaPathLocal, ubicacion)
                } else {
                    _uiState.value = _uiState.value.copy(enviando = false, error = e.message ?: "No se pudo finalizar el viaje.")
                }
            }
    }

    private suspend fun encolarFinalizarOffline(
        id: String,
        receptorNombre: String?,
        fotoUrl: String?,
        fotoPathLocal: String?,
        firmaUrl: String?,
        firmaPathLocal: String?,
        ubicacion: Pair<Double, Double>?,
    ) {
        app.pendingActionsRepository.encolarFinalizar(
            id, receptorNombre, fotoUrl, fotoPathLocal, firmaUrl, firmaPathLocal, ubicacion?.first, ubicacion?.second,
        )
        _uiState.value = _uiState.value.copy(enviando = false, finalizarEncolado = true)
    }

    /** Marca una parada intermedia como entregada (repartos con varias entregas en la misma vuelta). */
    fun marcarParadaEntregada(paradaId: String) {
        val id = _uiState.value.viaje?.id ?: return
        _uiState.value = _uiState.value.copy(enviando = true, error = null)
        viewModelScope.launch {
            app.pedidoRepository.marcarParadaEntregada(id, paradaId)
                .onSuccess { _uiState.value = _uiState.value.copy(enviando = false, viaje = it) }
                .onFailure { _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo marcar la parada como entregada.") }
        }
    }

    /** Botón "No se pudo entregar" (ej. el cliente no atendió) — el pedido no se anula, el admin lo puede reintentar. */
    fun marcarNoEntregado(motivo: String?) {
        val id = _uiState.value.viaje?.id ?: return
        _uiState.value = _uiState.value.copy(enviando = true, error = null)
        viewModelScope.launch {
            app.pedidoRepository.marcarNoEntregado(id, motivo)
                .onSuccess { _uiState.value = _uiState.value.copy(enviando = false, viaje = null, terminado = true) }
                .onFailure { _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo guardar — probá de nuevo.") }
        }
    }

    /** Nota de texto libre sobre el viaje (ej. "entregado en porteria a Fulano"), se ve en el detalle del panel. */
    fun agregarComentario(texto: String) {
        val id = _uiState.value.viaje?.id ?: return
        if (texto.isBlank()) return
        _uiState.value = _uiState.value.copy(enviandoComentario = true, error = null)
        viewModelScope.launch {
            app.pedidoRepository.agregarComentario(id, texto.trim())
                .onSuccess { _uiState.value = _uiState.value.copy(enviandoComentario = false) }
                .onFailure { _uiState.value = _uiState.value.copy(enviandoComentario = false, error = "No se pudo guardar el comentario.") }
        }
    }

    /**
     * "Reportar al cliente" (spec-antiabuso Fase 3): se acumula contra el teléfono, el admin lo
     * ve en el aviso del cliente. No bloquea nada por sí solo.
     */
    fun reportarCliente(tipo: String, nota: String?) {
        val id = _uiState.value.viaje?.id ?: return
        _uiState.value = _uiState.value.copy(enviandoReporte = true, error = null)
        viewModelScope.launch {
            app.pedidoRepository.reportarCliente(id, tipo, nota?.trim()?.ifBlank { null })
                .onSuccess { _uiState.value = _uiState.value.copy(enviandoReporte = false, reporteRegistrado = true) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        enviandoReporte = false,
                        error = "No se pudo guardar el reporte (¿ya reportaste lo mismo en este viaje?).",
                    )
                }
        }
    }

    fun cerrarConfirmacionReporte() {
        _uiState.value = _uiState.value.copy(reporteRegistrado = false)
    }

    /** Respuesta al popup "¿Seguís libre o te desactivás?" tras finalizar el último viaje activo. */
    fun resolverPreguntaLibre(seguirLibre: Boolean) {
        _uiState.value = _uiState.value.copy(preguntarSiSigueLibre = false)
        if (seguirLibre) return
        viewModelScope.launch {
            app.cadeteRepository.actualizarEstado(EstadoCadete.DESCONECTADO)
        }
    }

    /** Respuesta al popup "¿Seguís recibiendo pedidos o te ponés ocupado?" tras aceptar un viaje. */
    fun resolverPreguntaOcupado(ponerseOcupado: Boolean) {
        _uiState.value = _uiState.value.copy(preguntarSiQuedaOcupado = false)
        if (!ponerseOcupado) return
        viewModelScope.launch {
            app.cadeteRepository.actualizarEstado(EstadoCadete.OCUPADO)
        }
    }

    private fun escucharEventos() {
        viewModelScope.launch {
            app.realtimeManager.eventosViaje.collect { evento ->
                val actual = _uiState.value.viaje ?: return@collect
                if (evento.pedido.id != actual.id) return@collect
                when (evento.tipo) {
                    EventoViaje.VIAJE_QUITADO, EventoViaje.VIAJE_CANCELADO ->
                        _uiState.value = _uiState.value.copy(viaje = null, terminado = true, error = "Te quitaron este viaje.")
                }
            }
        }
    }
}
