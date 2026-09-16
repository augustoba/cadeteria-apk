package com.cadeteria.cadete.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Registrado en AndroidManifest.xml — es lo que Android instancia para dibujar/actualizar el widget. */
class CadeteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CadeteWidget()
}
