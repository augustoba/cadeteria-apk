package com.cadeteria.cadete.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cadeteria.cadete.CadeteApp

/**
 * Factory genérica para no depender de Hilt/Dagger (ver comentario en CadeteApp.kt):
 * cada ViewModel recibe la Application ya armada y arma sus dependencias desde ahí.
 */
class ViewModelFactory(
    private val app: CadeteApp,
    private val creator: (CadeteApp) -> ViewModel,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator(app) as T
}
