package com.cadeteria.cadete.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.ActualizacionCadeteRequestDto
import com.cadeteria.cadete.data.remote.dto.CadeteActualizacionCampoDto
import com.cadeteria.cadete.data.remote.dto.CadeteActualizacionDto
import com.cadeteria.cadete.data.remote.dto.CadeteConfigDto
import com.cadeteria.cadete.data.remote.dto.CadeteDto
import com.cadeteria.cadete.data.remote.dto.MiSemanaDto
import com.cadeteria.cadete.data.remote.mensajeDelServidor
import com.cadeteria.cadete.ui.theme.TemaApp
import com.cadeteria.cadete.util.Validaciones
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PerfilUiState(
    val guardandoPassword: Boolean = false,
    val guardandoTelefono: Boolean = false,
    val guardandoCuenta: Boolean = false,
    val guardandoActualizacion: Boolean = false,
    /** Qué campo de "Actualizar mis datos" está subiendo la foto ahora mismo (para el spinner puntual), o null. */
    val subiendoFoto: String? = null,
    val mensaje: String? = null,
    val error: String? = null,
)

class PerfilViewModel(private val app: CadeteApp) : ViewModel() {

    private val _cadete = MutableStateFlow<CadeteDto?>(null)
    val cadete: StateFlow<CadeteDto?> = _cadete

    private val _miSemana = MutableStateFlow<MiSemanaDto?>(null)
    val miSemana: StateFlow<MiSemanaDto?> = _miSemana

    /** Cuota semanal / % de comisión configurados — para mostrar "cuánto falta" en la ficha de pago (ronda 7). */
    private val _config = MutableStateFlow<CadeteConfigDto?>(null)
    val config: StateFlow<CadeteConfigDto?> = _config

    /** Historial de "Actualizar mis datos" (mejora 2026-09-23) — el más reciente primero. */
    private val _misActualizaciones = MutableStateFlow<List<CadeteActualizacionDto>>(emptyList())
    val misActualizaciones: StateFlow<List<CadeteActualizacionDto>> = _misActualizaciones

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState

    val tema: StateFlow<TemaApp> = app.sessionManager.tema
        .stateIn(viewModelScope, SharingStarted.Eagerly, TemaApp.SISTEMA)

    init {
        cargar()
    }

    fun cambiarTema(nuevoTema: TemaApp) {
        viewModelScope.launch { app.sessionManager.setTema(nuevoTema) }
    }

    fun cargar() {
        viewModelScope.launch {
            app.cadeteRepository.miPerfil().onSuccess { _cadete.value = it }
            app.cadeteRepository.miPagoSemanal().onSuccess { _miSemana.value = it }
            app.cadeteRepository.miConfiguracion().onSuccess { _config.value = it }
            app.cadeteRepository.misActualizaciones().onSuccess { _misActualizaciones.value = it }
        }
    }

    fun cambiarPassword(actual: String, nueva: String, onOk: () -> Unit) {
        if (!Validaciones.passwordValida(nueva)) {
            _uiState.value = _uiState.value.copy(error = Validaciones.MSJ_PASSWORD, mensaje = null)
            return
        }
        _uiState.value = _uiState.value.copy(guardandoPassword = true, error = null, mensaje = null)
        viewModelScope.launch {
            app.cadeteRepository.cambiarPassword(actual, nueva)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(guardandoPassword = false, mensaje = "Contraseña actualizada.")
                    onOk()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(guardandoPassword = false, error = it.mensajeDelServidor() ?: "No se pudo cambiar la contraseña — revisá tu conexión.")
                }
        }
    }

    fun actualizarTelefono(telefono: String) {
        if (!Validaciones.TELEFONO.matches(telefono)) {
            _uiState.value = _uiState.value.copy(error = Validaciones.MSJ_TELEFONO, mensaje = null)
            return
        }
        _uiState.value = _uiState.value.copy(guardandoTelefono = true, error = null, mensaje = null)
        viewModelScope.launch {
            app.cadeteRepository.actualizarTelefono(telefono)
                .onSuccess {
                    _cadete.value = it
                    _uiState.value = _uiState.value.copy(guardandoTelefono = false, mensaje = "Teléfono actualizado.")
                }
                .onFailure { _uiState.value = _uiState.value.copy(guardandoTelefono = false, error = it.mensajeDelServidor() ?: "No se pudo actualizar el teléfono.") }
        }
    }

    fun actualizarCuenta(cbu: String, aliasCbu: String) {
        val mal = Validaciones.problemas(
            !Validaciones.vacioO(Validaciones.CBU, cbu.trim()) to Validaciones.MSJ_CBU,
            !Validaciones.vacioO(Validaciones.ALIAS_CBU, aliasCbu.trim()) to Validaciones.MSJ_ALIAS,
        )
        if (mal != null) {
            _uiState.value = _uiState.value.copy(error = mal, mensaje = null)
            return
        }
        _uiState.value = _uiState.value.copy(guardandoCuenta = true, error = null, mensaje = null)
        viewModelScope.launch {
            app.cadeteRepository.actualizarCuenta(cbu.ifBlank { null }, aliasCbu.ifBlank { null })
                .onSuccess {
                    _cadete.value = it
                    _uiState.value = _uiState.value.copy(guardandoCuenta = false, mensaje = "Datos de cobro guardados.")
                }
                .onFailure { _uiState.value = _uiState.value.copy(guardandoCuenta = false, error = it.mensajeDelServidor() ?: "No se pudieron guardar los datos de cobro.") }
        }
    }

    /** Sube la foto a Cloudinary (mismo flujo que las fotos de viaje) y manda la propuesta al toque. */
    fun proponerFoto(campo: String, archivo: File) {
        viewModelScope.launch {
            val cloudName = _config.value?.cloudinaryCloudName
            val uploadPreset = _config.value?.cloudinaryUploadPreset
            if (cloudName == null || uploadPreset == null) {
                _uiState.value = _uiState.value.copy(error = "Todavía no cargó la configuración — probá de nuevo en un segundo.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(subiendoFoto = campo, error = null, mensaje = null)
            val resultado = app.cloudinaryUploader.subir(cloudName, uploadPreset, archivo)
            _uiState.value = _uiState.value.copy(subiendoFoto = null)
            val url = resultado.getOrNull()
            if (url == null) {
                _uiState.value = _uiState.value.copy(error = "No se pudo subir la foto — probá de nuevo.")
                return@launch
            }
            val req = when (campo) {
                "FOTO_PERFIL" -> ActualizacionCadeteRequestDto(fotoUrl = url)
                "FOTO_VEHICULO" -> ActualizacionCadeteRequestDto(fotoVehiculoUrl = url)
                "FOTO_TARJETA_VERDE" -> ActualizacionCadeteRequestDto(fotoTarjetaVerdeUrl = url)
                "FOTO_TARJETA_VERDE_DORSO" -> ActualizacionCadeteRequestDto(fotoTarjetaVerdeDorsoUrl = url)
                else -> return@launch
            }
            enviarActualizacion(req)
        }
    }

    fun proponerDatosVehiculo(marca: String, modelo: String, color: String, patente: String, anio: String) {
        val mal = Validaciones.problemas(
            !Validaciones.vacioO(Validaciones.MARCA_MODELO, marca.trim()) to Validaciones.MSJ_MARCA,
            !Validaciones.vacioO(Validaciones.MARCA_MODELO, modelo.trim()) to Validaciones.MSJ_MODELO,
            !Validaciones.vacioO(Validaciones.COLOR, color.trim()) to Validaciones.MSJ_COLOR,
            !Validaciones.vacioO(Validaciones.PATENTE_MOTO, patente) to Validaciones.MSJ_PATENTE,
            (anio.isNotBlank() && anio.toIntOrNull()?.let { it in 1950..2100 } != true) to "El año del vehículo no es válido.",
        )
        if (mal != null) {
            _uiState.value = _uiState.value.copy(error = mal, mensaje = null)
            return
        }
        viewModelScope.launch {
            enviarActualizacion(
                ActualizacionCadeteRequestDto(
                    vehiculoMarca = marca.ifBlank { null },
                    vehiculoModelo = modelo.ifBlank { null },
                    vehiculoColor = color.ifBlank { null },
                    vehiculoPatente = patente.ifBlank { null }?.let { Validaciones.normalizarPatente(it) },
                    vehiculoAnio = anio.toIntOrNull(),
                ),
            )
        }
    }

    private suspend fun enviarActualizacion(req: ActualizacionCadeteRequestDto) {
        _uiState.value = _uiState.value.copy(guardandoActualizacion = true, error = null, mensaje = null)
        app.cadeteRepository.crearActualizacion(req)
            .onSuccess {
                _uiState.value = _uiState.value.copy(guardandoActualizacion = false, mensaje = "Enviado — queda pendiente de revisión del admin.")
                app.cadeteRepository.misActualizaciones().onSuccess { lista -> _misActualizaciones.value = lista }
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(guardandoActualizacion = false, error = it.mensajeDelServidor() ?: "No se pudo enviar la actualización — revisá tu conexión.")
            }
    }

    /** Último estado (pendiente/rechazado) de un campo puntual, para el chip debajo de cada uno. */
    fun ultimoEstadoDe(lista: List<CadeteActualizacionDto>, campo: String): CadeteActualizacionCampoDto? =
        lista.firstOrNull()?.campos?.firstOrNull { it.campo == campo }

    fun hayAlgoPendiente(lista: List<CadeteActualizacionDto>): Boolean =
        lista.firstOrNull()?.campos?.any { it.estado == "PENDIENTE" } ?: false
}
