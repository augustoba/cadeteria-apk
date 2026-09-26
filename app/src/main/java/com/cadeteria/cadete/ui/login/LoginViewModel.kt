package com.cadeteria.cadete.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.mensajeDelServidor
import com.cadeteria.cadete.data.repository.VersionDesactualizadaException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val cargando: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val app: CadeteApp) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(username: String, password: String, onOk: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(error = "Completá usuario y contraseña.")
            return
        }
        _uiState.value = LoginUiState(cargando = true)
        viewModelScope.launch {
            app.authRepository.login(username, password)
                .onSuccess {
                    _uiState.value = LoginUiState(cargando = false)
                    app.mostrarBienvenidaAlEntrar = true
                    app.mostrarRecordatoriosAlEntrar = true
                    onOk()
                }
                .onFailure {
                    _uiState.value = LoginUiState(
                        cargando = false,
                        error = when {
                            it is VersionDesactualizadaException -> it.message
                            // El backend dice el motivo: contraseña mal, cuenta bloqueada por intentos,
                            // cuota semanal impaga o contraseña temporal vencida (2026-09-26).
                            else -> it.mensajeDelServidor() ?: "No se pudo conectar con el servidor. Revisá tu conexión."
                        },
                    )
                }
        }
    }
}
