package com.cadeteria.cadete.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cadeteria.cadete.data.remote.dto.EstadoCadete

/** Claves guardadas en las preferencias propias de cada instancia del widget (una por celular/pantalla). */
object CadeteWidgetKeys {
    val ESTADO_ID = stringPreferencesKey("estadoId")
    val NOMBRE = stringPreferencesKey("nombre")
    val MENSAJE = stringPreferencesKey("mensaje")
    val ACTUALIZANDO = booleanPreferencesKey("actualizando")
}

/**
 * Widget de pantalla de inicio (mejora 2026-09-16) — el primer gesto de cada turno del
 * cadete es activarse; esto le evita tener que abrir la app entera solo para eso. Estado
 * mínimo (guardado por Glance con `PreferencesGlanceStateDefinition`, uno por instancia
 * del widget): último estado conocido, nombre, y un mensaje corto si algo se lo impidió
 * (mismas reglas que el botón "Activarme" de HomeViewModel — viajes pendientes bloquean
 * desconectarse, checklist de documentación bloquea activarse si está prendido).
 */
class CadeteWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val estadoId = prefs[CadeteWidgetKeys.ESTADO_ID] ?: EstadoCadete.DESCONECTADO
            val nombre = prefs[CadeteWidgetKeys.NOMBRE]
            val mensaje = prefs[CadeteWidgetKeys.MENSAJE]
            val actualizando = prefs[CadeteWidgetKeys.ACTUALIZANDO] ?: false

            val activo = estadoId != EstadoCadete.DESCONECTADO
            val colorFondo = if (activo) Color(0xFF16A34A) else Color(0xFF6B7280)

            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(colorFondo))
                    .padding(12.dp)
                    .clickable(actionRunCallback<ToggleDisponibilidadAction>()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (nombre.isNullOrBlank()) "CADEM" else nombre,
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp),
                )
                Text(
                    when {
                        actualizando -> "Actualizando…"
                        activo -> "🟢 Activo — tocá para desconectarte"
                        else -> "🔴 Desconectado — tocá para activarte"
                    },
                    style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 14.sp),
                )
                if (!mensaje.isNullOrBlank()) {
                    Text(mensaje, style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp))
                }
            }
        }
    }

    companion object {
        suspend fun actualizarTodos(context: Context) {
            CadeteWidget().updateAll(context)
        }

        /**
         * Se llama desde HomeViewModel cada vez que el estado cambia DESDE LA APP (no
         * desde el widget) — así el widget no queda mostrando algo viejo si el cadete se
         * activó/desconectó abriendo la app en vez de tocando el widget. Puede haber más
         * de una instancia del widget (dos pantallas, por ejemplo), se actualizan todas.
         */
        suspend fun sincronizarEstado(context: Context, estadoId: String, nombre: String) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(CadeteWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[CadeteWidgetKeys.ESTADO_ID] = estadoId
                        this[CadeteWidgetKeys.NOMBRE] = nombre
                        this[CadeteWidgetKeys.ACTUALIZANDO] = false
                        remove(CadeteWidgetKeys.MENSAJE)
                    }
                }
            }
            if (ids.isNotEmpty()) actualizarTodos(context)
        }
    }
}
