package com.cadeteria.cadete.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.HistorialDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/** Rango para filtrar la sección "Finalizados" (auditoría UX 2026-09-13, punto 5). */
enum class RangoHistorial { HOY, SEMANA, TODO }

data class HistorialUiState(
    val cargando: Boolean = true,
    val historial: HistorialDto? = null,
    val error: String? = null,
    val rango: RangoHistorial = RangoHistorial.HOY,
)

/** yyyy-MM-dd de hoy, sin depender de java.time (minSdk 24 sin desugaring). */
private fun hoyIso(): String {
    val c = Calendar.getInstance()
    return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

/** yyyy-MM-dd de hace `dias` días. */
private fun haceDiasIso(dias: Int): String {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_MONTH, -dias)
    return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

/** Sección "Finalizados": listado de viajes finalizados + resumen (spec — pedido del cadete). */
class HistorialViewModel(private val app: CadeteApp) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cambiarRango(rango: RangoHistorial) {
        if (rango == _uiState.value.rango) return
        _uiState.value = _uiState.value.copy(rango = rango)
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            val (desde, hasta) = when (_uiState.value.rango) {
                RangoHistorial.HOY -> hoyIso() to hoyIso()
                RangoHistorial.SEMANA -> haceDiasIso(6) to hoyIso()
                RangoHistorial.TODO -> null to null
            }
            app.pedidoRepository.historial(desde, hasta)
                .onSuccess { _uiState.value = _uiState.value.copy(cargando = false, historial = it) }
                .onFailure { _uiState.value = _uiState.value.copy(cargando = false, error = "No se pudo cargar el historial.") }
        }
    }
}
