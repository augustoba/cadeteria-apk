package com.cadeteria.cadete.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.CadeteConfigDto
import com.cadeteria.cadete.data.remote.dto.CadeteDto
import com.cadeteria.cadete.data.remote.dto.MiSemanaDto
import com.cadeteria.cadete.ui.theme.TemaApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PerfilUiState(
    val guardandoPassword: Boolean = false,
    val guardandoTelefono: Boolean = false,
    val guardandoCuenta: Boolean = false,
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
}
