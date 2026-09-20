package com.cadeteria.cadete.ui.recuperarpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class PasoRecuperarPassword { PEDIR_USUARIO, PEDIR_CODIGO }

data class RecuperarPasswordUiState(
    val paso: PasoRecuperarPassword = PasoRecuperarPassword.PEDIR_USUARIO,
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    /** true = contraseña cambiada con éxito, la pantalla ya puede cerrarse. */
    val listo: Boolean = false,
)

/** "Olvidé mi contraseña" del cadete, sin sesión — dos pasos: pedir el código por mail, y cambiarla con ese código. */
class RecuperarPasswordViewModel(private val app: CadeteApp) : ViewModel() {

    private val _uiState = MutableStateFlow(RecuperarPasswordUiState())
    val uiState: StateFlow<RecuperarPasswordUiState> = _uiState

    private var username: String = ""

    fun pedirCodigo(username: String) {
        if (username.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Ingresá tu usuario.")
            return
        }
        this.username = username.trim()
        _uiState.value = _uiState.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            app.authRepository.recuperarPassword(this@RecuperarPasswordViewModel.username)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        paso = PasoRecuperarPassword.PEDIR_CODIGO,
                        mensaje = "Si el usuario existe y tiene un mail cargado, te va a llegar un código.",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        error = "No se pudo pedir el código — revisá tu conexión e intentá de nuevo.",
                    )
                }
        }
    }

    fun confirmar(codigo: String, nuevaPassword: String, repetirPassword: String) {
        if (codigo.isBlank() || nuevaPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completá el código y la contraseña nueva.")
            return
        }
        if (nuevaPassword != repetirPassword) {
            _uiState.value = _uiState.value.copy(error = "Las contraseñas no coinciden.")
            return
        }
        _uiState.value = _uiState.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            app.authRepository.confirmarRecuperarPassword(username, codigo, nuevaPassword)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        listo = true,
                        mensaje = "Contraseña actualizada — ya podés entrar con la nueva.",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "Código inválido o vencido.")
                }
        }
    }

    /** Botón explícito para corregir el usuario (ej. typo) — distinto del back del sistema, que en este paso no hace nada. */
    fun volverAPedirUsuario() {
        _uiState.value = RecuperarPasswordUiState(paso = PasoRecuperarPassword.PEDIR_USUARIO)
    }
}
