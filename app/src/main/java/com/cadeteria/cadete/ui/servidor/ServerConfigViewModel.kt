package com.cadeteria.cadete.ui.servidor

import androidx.lifecycle.ViewModel
import com.cadeteria.cadete.CadeteApp

class ServerConfigViewModel(private val app: CadeteApp) : ViewModel() {
    suspend fun cargarActual(): String = app.sessionManager.baseUrlSync()
    suspend fun guardar(url: String) = app.sessionManager.setBaseUrl(url)
}
