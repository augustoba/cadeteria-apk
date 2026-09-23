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
import com.cadeteria.cadete.ui.theme.TemaApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

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
        _uiState.value = _uiState.value.copy(guardandoPassword = true, error = null, mensaje = null)
        viewModelScope.launch {
            app.cadeteRepository.cambiarPassword(actual, nueva)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(guardandoPassword = false, mensaje = "Contraseña actualizada.")
                    onOk()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(guardandoPassword = false, error = "No se pudo cambiar la contraseña — revisá la actual.")
                }
        }
    }

    fun actualizarTelefono(telefono: String) {
        _uiState.value = _uiState.value.copy(guardandoTelefono = true, error = null, mensaje = null)
        viewModelScope.launch {
            app.cadeteRepository.actualizarTelefono(telefono)
                .onSuccess {
                    _cadete.value = it
                    _uiState.value = _uiState.value.copy(guardandoTelefono = false, mensaje = "Teléfono actualizado.")
                }
                .onFailure { _uiState.value = _uiState.value.copy(guardandoTelefono = false, error = "No se pudo actualizar el teléfono.") }
        }
    }

    fun actualizarCuenta(cbu: String, aliasCbu: String) {
        _uiState.value = _uiState.value.copy(guardandoCuenta = true, error = null, mensaje = null)
        viewModelScope.launch {
            app.cadeteRepository.actualizarCuenta(cbu.ifBlank { null }, aliasCbu.ifBlank { null })
                .onSuccess {
                    _cadete.value = it
                    _uiState.value = _uiState.value.copy(guardandoCuenta = false, mensaje = "Datos de cobro guardados.")
                }
                .onFailure { _uiState.value = _uiState.value.copy(guardandoCuenta = false, error = "No se pudieron guardar los datos de cobro.") }
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
        viewModelScope.launch {
            enviarActualizacion(
                ActualizacionCadeteRequestDto(
                    vehiculoMarca = marca.ifBlank { null },
                    vehiculoModelo = modelo.ifBlank { null },
                    vehiculoColor = color.ifBlank { null },
                    vehiculoPatente = patente.ifBlank { null },
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
                _uiState.value = _uiState.value.copy(guardandoActualizacion = false, error = "No se pudo enviar la actualización — puede que ya tengas una pendiente de revisión.")
            }
    }

    /** Último estado (pendiente/rechazado) de un campo puntual, para el chip debajo de cada uno. */
    fun ultimoEstadoDe(campo: String): CadeteActualizacionCampoDto? =
        _misActualizaciones.value.firstOrNull()?.campos?.firstOrNull { it.campo == campo }

    fun hayAlgoPendiente(): Boolean =
        _misActualizaciones.value.firstOrNull()?.campos?.any { it.estado == "PENDIENTE" } ?: false
}
