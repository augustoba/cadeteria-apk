package com.cadeteria.cadete.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.EstadoCadete
import com.cadeteria.cadete.location.LocationServiceController

/**
 * Toca el widget: activarse/desconectarse sin abrir la app (mejora 2026-09-16). Reproduce
 * las mismas 2 reglas que ya tiene `HomeViewModel.toggleDisponibilidad` — no lo reusa
 * directo porque un ViewModel de Compose no se puede instanciar acá, pero la lógica es
 * chica y vale la pena tenerla clara en los dos lados antes que agregar una capa de
 * indirección para 6 líneas.
 */
class ToggleDisponibilidadAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as CadeteApp

        marcarActualizando(context, glanceId, true)

        val perfil = app.cadeteRepository.miPerfil().getOrNull()
        if (perfil == null) {
            guardarMensaje(context, glanceId, "No se pudo conectar — abrí la app.")
            return
        }

        val nuevoEstado = if (perfil.estado.id == EstadoCadete.DESCONECTADO) EstadoCadete.LIBRE else EstadoCadete.DESCONECTADO

        if (nuevoEstado == EstadoCadete.DESCONECTADO) {
            val activos = app.pedidoRepository.viajesActivos().getOrDefault(emptyList())
            if (activos.isNotEmpty()) {
                guardarMensaje(context, glanceId, "Tenés viajes sin finalizar — abrí la app.", perfil.estado.id, perfil.nombre)
                return
            }
        }

        if (nuevoEstado == EstadoCadete.LIBRE) {
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            if (config?.checklistDocumentacionObligatorio == true) {
                val faltaAlgo = perfil.fotoCarnetUrl.isNullOrBlank() ||
                    perfil.fotoTarjetaVerdeUrl.isNullOrBlank() ||
                    perfil.fotoVehiculoUrl.isNullOrBlank()
                if (faltaAlgo) {
                    guardarMensaje(context, glanceId, "Te falta documentación — abrí la app.", perfil.estado.id, perfil.nombre)
                    return
                }
            }
        }

        app.cadeteRepository.actualizarEstado(nuevoEstado)
            .onSuccess { actualizado ->
                if (nuevoEstado == EstadoCadete.DESCONECTADO) {
                    LocationServiceController.detener(context)
                } else {
                    LocationServiceController.iniciar(context)
                }
                guardarMensaje(context, glanceId, null, actualizado.estado.id, actualizado.nombre)
            }
            .onFailure {
                guardarMensaje(context, glanceId, "No se pudo actualizar tu estado.", perfil.estado.id, perfil.nombre)
            }
    }

    private suspend fun marcarActualizando(context: Context, glanceId: GlanceId, valor: Boolean) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply { this[CadeteWidgetKeys.ACTUALIZANDO] = valor }
        }
        CadeteWidget.actualizarTodos(context)
    }

    private suspend fun guardarMensaje(context: Context, glanceId: GlanceId, mensaje: String?, estadoId: String? = null, nombre: String? = null) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[CadeteWidgetKeys.ACTUALIZANDO] = false
                if (mensaje != null) this[CadeteWidgetKeys.MENSAJE] = mensaje else remove(CadeteWidgetKeys.MENSAJE)
                if (estadoId != null) this[CadeteWidgetKeys.ESTADO_ID] = estadoId
                if (nombre != null) this[CadeteWidgetKeys.NOMBRE] = nombre
            }
        }
        CadeteWidget.actualizarTodos(context)
    }
}
