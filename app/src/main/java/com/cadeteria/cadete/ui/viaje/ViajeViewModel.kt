package com.cadeteria.cadete.ui.viaje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.local.ParadaPendiente
import com.cadeteria.cadete.data.remote.dto.EstadoCadete
import com.cadeteria.cadete.widget.CadeteWidget
import com.cadeteria.cadete.data.remote.dto.EventoViaje
import com.cadeteria.cadete.data.remote.dto.MarcaEnLugar
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.data.remote.mensajeDelServidor
import com.cadeteria.cadete.location.ControlEnLugar
import com.cadeteria.cadete.location.UbicacionMarcada
import com.cadeteria.cadete.location.ubicacionPrecisa
import com.cadeteria.cadete.util.Validaciones
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * El control "en el lugar" no dejó marcar (2026-09-28): la pantalla muestra el mensaje y el botón
 * "Estoy en el lugar". tieneFoto = la acción ya traía foto (no hace falta sacar otra).
 */
data class FueraDeZona(val mensaje: String, val tieneFoto: Boolean)

data class ViajeUiState(
    val cargando: Boolean = true,
    val viaje: PedidoDto? = null,
    val enviando: Boolean = false,
    val error: String? = null,
    /** true cuando el viaje se cerró (finalizado/quitado/cancelado) — la pantalla se puede cerrar. */
    val terminado: Boolean = false,
    /** true cuando este era el último viaje activo del cadete — se le pregunta cómo quiere quedar: Libre, Ocupado o Desconectado (a pedido del dueño). */
    val preguntarSiSigueLibre: Boolean = false,
    /** Estado que tenía al finalizar (EstadoCadete), para marcarlo en ese cartel; null si no se pudo consultar. */
    val estadoAlFinalizar: String? = null,
    /** true recién aceptado un viaje — se le pregunta si quiere seguir recibiendo pedidos o ponerse OCUPADO (a pedido del dueño). */
    val preguntarSiQuedaOcupado: Boolean = false,
    /** true cuando "Finalizar" se encoló porque no había conexión — la pantalla queda mostrando el aviso
     * hasta que el cadete toca "Volver" (no cierra sola, para que el aviso no pase desapercibido). */
    val finalizarEncolado: Boolean = false,
    /** Mismo caso que finalizarEncolado pero para "Marcar como retirado" (ronda 3, punto 24). */
    val retiradoEncolado: Boolean = false,
    /** Paradas marcadas sin señal (2026-09-28): se muestran como entregadas hasta que el envío se complete. */
    val paradasEncoladas: Set<String> = emptySet(),
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
    /** Base del link de seguimiento, para el QR que se le muestra al cliente (2026-09-25). */
    val urlSeguimientoBase: String = "",
    /** Esperando que el GPS fije antes de marcar (sin datos tarda más): "Buscando tu ubicación…". */
    val buscandoUbicacion: Boolean = false,
    val fueraDeZona: FueraDeZona? = null,
    /** Control "en el lugar": vienen de la configuración del backend (0 = backend viejo, van los defaults). */
    val enLugarRadioM: Int = ControlEnLugar.RADIO_M_DEFAULT,
    val enLugarPrecisionMaxM: Int = ControlEnLugar.PRECISION_MAX_M_DEFAULT,
    /** Interruptor de Configuración: apagado, no se frena (solo el orden Retirado → Entregado). */
    val enLugarControlActivo: Boolean = true,
    /** Hora del toque del Retirado guardado sin señal: para la espera antes de Finalizar (el viaje no la tiene todavía). */
    val retiroEncoladoEn: String? = null,
    /** Minutos mínimos entre Retirado y Finalizar (2026-09-29, Configuración); 0 = sin espera. */
    val minutosMinimosEntrega: Int = EsperaEntrega.MINUTOS_DEFAULT,
    /** Tilde animado (2026-09-29): "¡Viaje aceptado!" o "¡Entregado!"; los diálogos de después esperan a que termine. */
    val festejo: Festejo? = null,
) {
    /** Desde cuándo corre la espera para poder finalizar: el Retirado del servidor o el guardado sin señal. */
    val retiradoDesde: String? get() = viaje?.retiradoEn?.takeIf { it.isNotBlank() } ?: retiroEncoladoEn
}

/** El tilde animado que se muestra después de aceptar o de entregar (2026-09-29). */
sealed class Festejo {
    data object Aceptado : Festejo()
    data class Entregado(val monto: Double) : Festejo()
}

class ViajeViewModel(private val app: CadeteApp, private val pedidoId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(ViajeUiState())
    val uiState: StateFlow<ViajeUiState> = _uiState.asStateFlow()

    fun terminoFestejo() {
        _uiState.value = _uiState.value.copy(festejo = null)
    }

    /** Lo que el cadete quiso marcar: Retirado, una parada o Entregado. */
    private sealed class Accion {
        abstract val foto: File?

        data class Retiro(override val foto: File?) : Accion()
        data class Parada(val paradaId: String, override val foto: File?) : Accion()
        data class Entrega(val receptorNombre: String?, override val foto: File?, val firma: File?) : Accion()
    }

    /** La acción que el control no dejó marcar, con la ubicación y la hora del toque originales. */
    private data class Pendiente(val accion: Accion, val ubicacion: UbicacionMarcada?, val tocadoEn: String)

    private var pendiente: Pendiente? = null

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
                    urlSeguimientoBase = it.urlSeguimientoBase,
                    enLugarRadioM = it.enLugarRadioM.takeIf { r -> r > 0 } ?: ControlEnLugar.RADIO_M_DEFAULT,
                    enLugarPrecisionMaxM = it.enLugarPrecisionMaxM.takeIf { p -> p > 0 } ?: ControlEnLugar.PRECISION_MAX_M_DEFAULT,
                    enLugarControlActivo = it.enLugarControlActivo != false,
                    // null = backend anterior a la espera: no se frena en el teléfono (el backend tampoco).
                    minutosMinimosEntrega = it.minutosMinimosRetiroEntrega ?: 0,
                )
            }
        }
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            val res = app.pedidoRepository.detalle(pedidoId)
            val viaje = res.getOrNull()
            // Lo guardado sin señal sale de la cola, no de la memoria de esta pantalla (bug 2026-09-29: al
            // volver a entrar desde Inicio se podía marcar de nuevo un Retirado/Finalizar ya encolado).
            val encolado = app.pendingActionsRepository.encoladoDe(pedidoId)
            // Sin pedir la ruta (2026-09-28): el mini mapa muestra solo los pines, el camino lo arma Maps/Waze.
            _uiState.value = _uiState.value.copy(
                cargando = false,
                viaje = viaje,
                // Sin señal no llega el detalle: si la entrega está guardada se muestra el aviso, no se cierra.
                terminado = viaje == null && !encolado.finalizar,
                finalizarEncolado = encolado.finalizar,
                retiradoEncolado = encolado.retiro != null,
                retiroEncoladoEn = encolado.retiro?.tocadoEn,
                paradasEncoladas = encolado.paradas,
            )
        }
    }

    fun aceptar() {
        val id = _uiState.value.viaje?.id ?: return
        _uiState.value = _uiState.value.copy(enviando = true)
        viewModelScope.launch {
            app.pedidoRepository.aceptar(id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        viaje = it, enviando = false, preguntarSiQuedaOcupado = true, festejo = Festejo.Aceptado,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        error = it.mensajeDelServidor() ?: "No se pudo aceptar — revisá tu conexión y probá de nuevo.",
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
                .onFailure { _uiState.value = _uiState.value.copy(enviando = false, error = it.mensajeDelServidor() ?: "No se pudo rechazar el viaje.") }
        }
    }

    /**
     * Botón "Retirado" — la foto es opcional salvo que Configuración la exija. Solo se puede marcar
     * estando en el lugar (2026-09-28, ver [iniciar]). Sin conexión se encola y la app lo reintenta
     * sola cuando vuelva internet (ronda 3, punto 24).
     */
    fun marcarRetirado(foto: File?) {
        if (_uiState.value.retiradoEncolado || _uiState.value.finalizarEncolado) return // ya guardado sin señal
        iniciar(Accion.Retiro(foto))
    }

    /** Marca una parada intermedia como entregada (repartos con varias entregas en la misma vuelta). */
    fun marcarParadaEntregada(paradaId: String) {
        if (paradaId in _uiState.value.paradasEncoladas || _uiState.value.finalizarEncolado) return
        iniciar(Accion.Parada(paradaId, null))
    }

    /**
     * Botón "Finalizar" (Entregado): solo en el destino y con el Retirado ya marcado. Sin conexión
     * (ni para subir la foto, ni para el POST) se encola y la app lo reintenta sola.
     */
    fun finalizar(receptorNombre: String?, foto: File?, firma: File?) {
        if (!Validaciones.vacioO(Validaciones.NOMBRE_PERSONA, receptorNombre)) {
            _uiState.value = _uiState.value.copy(error = Validaciones.MSJ_RECEPTOR)
            return
        }
        val s = _uiState.value
        if (s.finalizarEncolado) return // ya guardado sin señal
        val faltan = EsperaEntrega.segundosRestantes(s.retiradoDesde, System.currentTimeMillis(), s.minutosMinimosEntrega)
        if (faltan > 0) {
            _uiState.value = s.copy(error = EsperaEntrega.mensaje(faltan, s.minutosMinimosEntrega))
            return
        }
        iniciar(Accion.Entrega(receptorNombre, foto, firma))
    }

    /**
     * "Estoy en el lugar" (2026-09-28): el control no dejaba marcar (típico: la dirección del pedido
     * está mal ubicada en el mapa). Con foto obligatoria marca igual y queda "fuera de zona" — nadie
     * lo aprueba. Se mandan la ubicación y la hora del primer toque.
     */
    fun estoyEnElLugar(fotoNueva: File?) {
        val p = pendiente ?: return
        val accion = when (val a = p.accion) {
            is Accion.Retiro -> a.copy(foto = fotoNueva ?: a.foto)
            is Accion.Parada -> a.copy(foto = fotoNueva ?: a.foto)
            is Accion.Entrega -> a.copy(foto = fotoNueva ?: a.foto)
        }
        if (accion.foto == null) {
            _uiState.value = _uiState.value.copy(error = "Para marcar con \"Estoy en el lugar\" hace falta una foto.")
            return
        }
        pendiente = null
        _uiState.value = _uiState.value.copy(fueraDeZona = null, enviando = true, error = null)
        viewModelScope.launch { ejecutar(accion, p.ubicacion, p.tocadoEn, enElLugar = true) }
    }

    fun cancelarFueraDeZona() {
        pendiente = null
        _uiState.value = _uiState.value.copy(fueraDeZona = null)
    }

    /**
     * Control "en el lugar" (2026-09-28): espera un fix del GPS y compara con el punto (origen, la
     * parada o el destino). Lejos o sin ubicación → no marca y ofrece "Estoy en el lugar". GPS falso →
     * no marca y se le avisa al backend para que quede registrado. El backend vuelve a controlar.
     */
    private fun iniciar(accion: Accion) {
        val viaje = _uiState.value.viaje ?: return
        val tocadoEn = ControlEnLugar.ahoraIso()
        pendiente = null
        _uiState.value = _uiState.value.copy(enviando = true, buscandoUbicacion = true, error = null, fueraDeZona = null)
        viewModelScope.launch {
            val controlActivo = _uiState.value.enLugarControlActivo
            // Con el control apagado no hace falta esperar tanto al GPS: la posición es solo un dato.
            val ubicacion = ubicacionPrecisa(app, esperaMs = if (controlActivo) 30_000 else 8_000)
            _uiState.value = _uiState.value.copy(buscandoUbicacion = false)
            if (!controlActivo) {
                // Control apagado desde Configuración: se marca igual; el backend anota distancia y GPS falso.
                ejecutar(accion, ubicacion, tocadoEn, enElLugar = false, simulada = ubicacion?.simulada == true)
                return@launch
            }
            val (puntoLat, puntoLng, nombrePunto) = when (accion) {
                is Accion.Retiro -> Triple(viaje.origenLat, viaje.origenLng, "retiro")
                is Accion.Entrega -> Triple(viaje.destinoLat, viaje.destinoLng, "destino")
                is Accion.Parada -> viaje.paradas.firstOrNull { it.id == accion.paradaId }
                    ?.let { Triple(it.lat, it.lng, "punto de la parada") }
                    ?: Triple(viaje.destinoLat, viaje.destinoLng, "destino")
            }
            val s = _uiState.value
            when (val r = ControlEnLugar.evaluar(ubicacion, puntoLat, puntoLng, s.enLugarRadioM, s.enLugarPrecisionMaxM)) {
                is ControlEnLugar.Resultado.Ok -> ejecutar(accion, ubicacion, tocadoEn, enElLugar = false)
                ControlEnLugar.Resultado.Simulada -> avisarUbicacionSimulada(viaje.id, accion, ubicacion, tocadoEn)
                is ControlEnLugar.Resultado.Lejos -> frenarFueraDeZona(
                    accion, ubicacion, tocadoEn,
                    "Estás a ${ControlEnLugar.textoDistancia(r.distanciaM)} del $nombrePunto. " +
                        "Acercate, o si ya estás en el lugar (la dirección puede estar mal ubicada en el mapa), tocá \"Estoy en el lugar\" y sacá una foto.",
                )
                ControlEnLugar.Resultado.SinUbicacion -> frenarFueraDeZona(
                    accion, ubicacion, tocadoEn,
                    "No pudimos leer tu ubicación (revisá que el GPS esté prendido y probá de nuevo). " +
                        "Si ya estás en el lugar, tocá \"Estoy en el lugar\" y sacá una foto.",
                )
            }
        }
    }

    private fun frenarFueraDeZona(accion: Accion, ubicacion: UbicacionMarcada?, tocadoEn: String, mensaje: String) {
        pendiente = Pendiente(accion, ubicacion, tocadoEn)
        _uiState.value = _uiState.value.copy(enviando = false, fueraDeZona = FueraDeZona(mensaje, accion.foto != null))
    }

    /** No se marca: solo se le avisa al backend (sin foto) para que quede en el pedido y en la ficha del cadete. */
    private suspend fun avisarUbicacionSimulada(id: String, accion: Accion, ubicacion: UbicacionMarcada?, tocadoEn: String) {
        val marca = MarcaEnLugar(tocadoEn, enElLugar = false, ubicacionSimulada = true)
        when (accion) {
            is Accion.Retiro -> app.pedidoRepository.marcarRetirado(
                id, null, ubicacion?.lat, ubicacion?.lng, precision = ubicacion?.precisionM, marca = marca,
            )
            is Accion.Parada -> app.pedidoRepository.marcarParadaEntregada(
                id, accion.paradaId, ubicacion?.lat, ubicacion?.lng, ubicacion?.precisionM, null, marca,
            )
            is Accion.Entrega -> app.pedidoRepository.finalizar(
                id, accion.receptorNombre, null, null, ubicacion?.lat, ubicacion?.lng, precision = ubicacion?.precisionM, marca = marca,
            )
        }
        _uiState.value = _uiState.value.copy(
            enviando = false,
            error = "Tu celular está usando una ubicación simulada (GPS falso). Desactivá esa app para poder marcar. Quedó registrado.",
        )
    }

    private suspend fun ejecutar(
        accion: Accion, ubicacion: UbicacionMarcada?, tocadoEn: String, enElLugar: Boolean, simulada: Boolean = false,
    ) {
        val id = _uiState.value.viaje?.id ?: return
        val marca = MarcaEnLugar(tocadoEn, enElLugar, simulada)
        when (accion) {
            is Accion.Retiro -> ejecutarRetirado(id, accion.foto, ubicacion, marca)
            is Accion.Parada -> ejecutarParada(id, accion, ubicacion, marca)
            is Accion.Entrega -> ejecutarEntrega(id, accion, ubicacion, marca)
        }
    }

    private suspend fun ejecutarRetirado(id: String, foto: File?, ubicacion: UbicacionMarcada?, marca: MarcaEnLugar) {
        var fotoUrl: String? = null
        if (foto != null) {
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            if (config == null) {
                // Sin conexión ni para leer la configuración: se encola con la foto local.
                encolarRetiradoOffline(id, null, foto.absolutePath, ubicacion, marca)
                return
            }
            val subida = app.cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, foto)
            if (subida.isFailure) {
                if (subida.exceptionOrNull() is java.io.IOException) {
                    encolarRetiradoOffline(id, null, foto.absolutePath, ubicacion, marca)
                } else {
                    _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo subir la foto.")
                }
                return
            }
            fotoUrl = subida.getOrNull()
        }
        app.pedidoRepository.marcarRetirado(
            id, fotoUrl, ubicacion?.lat, ubicacion?.lng, precision = ubicacion?.precisionM,
            calleDetectada = ubicacion?.calle?.calle, localidadDetectada = ubicacion?.calle?.localidad, marca = marca,
        )
            .onSuccess {
                com.cadeteria.cadete.ui.common.Vibracion.exito(app)
                _uiState.value = _uiState.value.copy(enviando = false, viaje = it)
            }
            .onFailure { e ->
                if (e is java.io.IOException) {
                    encolarRetiradoOffline(id, fotoUrl, null, ubicacion, marca)
                } else {
                    _uiState.value = _uiState.value.copy(enviando = false, error = e.mensajeDelServidor() ?: "No se pudo marcar como retirado.")
                }
            }
    }

    private suspend fun encolarRetiradoOffline(
        id: String, fotoUrl: String?, fotoPathLocal: String?, ubicacion: UbicacionMarcada?, marca: MarcaEnLugar,
    ) {
        app.pendingActionsRepository.encolarRetirado(
            id, fotoUrl, fotoPathLocal, ubicacion?.lat, ubicacion?.lng, marca.tocadoEn, ubicacion?.precisionM, marca.enElLugar,
        )
        _uiState.value = _uiState.value.copy(enviando = false, retiradoEncolado = true, retiroEncoladoEn = marca.tocadoEn)
    }

    private suspend fun ejecutarParada(id: String, accion: Accion.Parada, ubicacion: UbicacionMarcada?, marca: MarcaEnLugar) {
        var fotoUrl: String? = null
        var fotoPathLocal: String? = null
        if (accion.foto != null) {
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            fotoUrl = config?.let {
                app.cloudinaryUploader.subir(it.cloudinaryCloudName, it.cloudinaryUploadPreset, accion.foto).getOrNull()
            }
            if (fotoUrl == null) fotoPathLocal = accion.foto.absolutePath
        }
        val encolar = suspend {
            app.pendingActionsRepository.encolarParada(
                ParadaPendiente(
                    id, accion.paradaId, fotoUrl, fotoPathLocal, ubicacion?.lat, ubicacion?.lng,
                    marca.tocadoEn, ubicacion?.precisionM, marca.enElLugar,
                ),
            )
            _uiState.value = _uiState.value.copy(
                enviando = false, paradasEncoladas = _uiState.value.paradasEncoladas + accion.paradaId,
            )
        }
        if (fotoPathLocal != null) {
            encolar()
            return
        }
        app.pedidoRepository.marcarParadaEntregada(id, accion.paradaId, ubicacion?.lat, ubicacion?.lng, ubicacion?.precisionM, fotoUrl, marca)
            .onSuccess {
                com.cadeteria.cadete.ui.common.Vibracion.exito(app)
                _uiState.value = _uiState.value.copy(enviando = false, viaje = it)
            }
            .onFailure { e ->
                if (e is java.io.IOException) {
                    encolar()
                } else {
                    _uiState.value = _uiState.value.copy(
                        enviando = false, error = e.mensajeDelServidor() ?: "No se pudo marcar la parada como entregada.",
                    )
                }
            }
    }

    private suspend fun ejecutarEntrega(id: String, accion: Accion.Entrega, ubicacion: UbicacionMarcada?, marca: MarcaEnLugar) {
        var fotoUrl: String? = null
        var fotoPathLocal: String? = null
        var firmaUrl: String? = null
        var firmaPathLocal: String? = null
        if (accion.foto != null || accion.firma != null) {
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            if (accion.foto != null) {
                val resultado = config?.let { app.cloudinaryUploader.subir(it.cloudinaryCloudName, it.cloudinaryUploadPreset, accion.foto) }
                if (resultado != null && resultado.isFailure && resultado.exceptionOrNull() !is java.io.IOException) {
                    _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo subir la foto.")
                    return
                }
                fotoUrl = resultado?.getOrNull()
                fotoPathLocal = if (fotoUrl == null) accion.foto.absolutePath else null
            }
            if (accion.firma != null) {
                val resultado = config?.let {
                    app.cloudinaryUploader.subir(it.cloudinaryCloudName, it.cloudinaryUploadPreset, accion.firma, "image/jpeg")
                }
                if (resultado != null && resultado.isFailure && resultado.exceptionOrNull() !is java.io.IOException) {
                    _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo subir la firma.")
                    return
                }
                firmaUrl = resultado?.getOrNull()
                firmaPathLocal = if (firmaUrl == null) accion.firma.absolutePath else null
            }
        }
        if (fotoPathLocal != null || firmaPathLocal != null) {
            encolarFinalizarOffline(id, accion.receptorNombre, fotoUrl, fotoPathLocal, firmaUrl, firmaPathLocal, ubicacion, marca)
            return
        }
        app.pedidoRepository.finalizar(
            id, accion.receptorNombre, fotoUrl, firmaUrl, ubicacion?.lat, ubicacion?.lng, precision = ubicacion?.precisionM,
            calleDetectada = ubicacion?.calle?.calle, localidadDetectada = ubicacion?.calle?.localidad, marca = marca,
        )
            .onSuccess {
                val quedanActivos = app.pedidoRepository.viajesActivos().getOrNull()?.isNotEmpty() ?: true
                val estadoActual = if (quedanActivos) null else app.cadeteRepository.miPerfil().getOrNull()?.estado?.id
                _uiState.value = _uiState.value.copy(
                    enviando = false,
                    viaje = it,
                    terminado = true,
                    preguntarSiSigueLibre = !quedanActivos,
                    estadoAlFinalizar = estadoActual,
                    festejo = Festejo.Entregado(it.precio),
                )
            }
            .onFailure { e ->
                if (e is java.io.IOException) {
                    encolarFinalizarOffline(id, accion.receptorNombre, fotoUrl, null, firmaUrl, null, ubicacion, marca)
                } else {
                    _uiState.value = _uiState.value.copy(enviando = false, error = e.mensajeDelServidor() ?: "No se pudo finalizar el viaje.")
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
        ubicacion: UbicacionMarcada?,
        marca: MarcaEnLugar,
    ) {
        app.pendingActionsRepository.encolarFinalizar(
            id, receptorNombre, fotoUrl, fotoPathLocal, firmaUrl, firmaPathLocal, ubicacion?.lat, ubicacion?.lng,
            marca.tocadoEn, ubicacion?.precisionM, marca.enElLugar,
        )
        _uiState.value = _uiState.value.copy(enviando = false, finalizarEncolado = true)
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

    /**
     * Respuesta al cartel "¿Cómo querés quedar?" tras finalizar el último viaje activo. Siempre se
     * manda el estado elegido (2026-10-03): antes "Seguir disponible" solo cerraba el cartel, y el
     * que se había puesto Ocupado al aceptar seguía Ocupado creyendo que le iban a llegar viajes.
     * El cartel se cierra recién cuando el servidor contesta; si falla, queda abierto con el error.
     */
    fun resolverPreguntaLibre(estado: String) {
        if (_uiState.value.enviando) return
        _uiState.value = _uiState.value.copy(enviando = true, error = null)
        viewModelScope.launch {
            app.cadeteRepository.actualizarEstado(estado)
                .onSuccess { actualizado ->
                    CadeteWidget.sincronizarEstado(app, actualizado.estado.id, actualizado.nombre)
                    app.recordatorioEstado.actualizar(actualizado.estado.id)
                    _uiState.value = _uiState.value.copy(enviando = false, preguntarSiSigueLibre = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        error = e.mensajeDelServidor() ?: "No se pudo cambiar tu estado — revisá tu conexión y probá de nuevo.",
                    )
                }
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
                    // Reclamo del cliente (2026-09-25): recargar para que aparezca el recuadro rojo.
                    EventoViaje.RECLAMO_CLIENTE -> cargar()
                }
            }
        }
    }
}
