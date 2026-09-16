package com.cadeteria.cadete.ui.avisos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.AvisoHistorialDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AvisosUiState(
    val cargando: Boolean = true,
    val avisos: List<AvisoHistorialDto> = emptyList(),
    val error: String? = null,
)

/** Pantalla "Avisos" con historial (mejora 2026-09-16) — antes un aviso general que ya se marcaba leído desaparecía para siempre, sin forma de volver a verlo. */
class AvisosViewModel(private val app: CadeteApp) : ViewModel() {

    private val _uiState = MutableStateFlow(AvisosUiState())
    val uiState: StateFlow<AvisosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            app.cadeteRepository.historialAvisos()
                .onSuccess { avisos -> _uiState.value = _uiState.value.copy(cargando = false, avisos = avisos) }
                .onFailure { _uiState.value = _uiState.value.copy(cargando = false, error = "No se pudieron cargar los avisos.") }
        }
    }
}
