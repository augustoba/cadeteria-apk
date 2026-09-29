package com.cadeteria.cadete.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.data.remote.dto.TipoAvisoCalle
import com.cadeteria.cadete.ui.home.AvisosCalleTexto
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Pin circular de color con borde blanco para diferenciar origen/destino/cadete de un vistazo.
 * Bitmap y no ShapeDrawable (2026-09-28): el ShapeDrawable no tiene tamaño propio (intrinsicWidth
 * -1) y el Marker de osmdroid lo dibujaba con ese tamaño, o sea invisible — el mapa no mostraba pines.
 */
fun pinDrawable(context: Context, color: Int, sizeDp: Int = 18): BitmapDrawable {
    val px = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(12)
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radio = px / 2f
    paint.color = Color.WHITE
    canvas.drawCircle(radio, radio, radio, paint)
    paint.color = color
    canvas.drawCircle(radio, radio, radio * 0.75f, paint)
    return BitmapDrawable(context.resources, bitmap)
}

/** Ícono de un aviso de la calle: el emoji del tipo (🚓 🚧 💥 ✊) sobre un círculo blanco con borde. */
fun pinAvisoCalle(context: Context, tipo: String, sizeDp: Int = 34): BitmapDrawable {
    val px = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radio = px / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.WHITE
    canvas.drawCircle(radio, radio, radio, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = px * 0.06f
    paint.color = Color.parseColor("#D32F2F")
    canvas.drawCircle(radio, radio, radio - paint.strokeWidth, paint)
    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = px * 0.55f
        textAlign = Paint.Align.CENTER
    }
    val y = radio - (texto.descent() + texto.ascent()) / 2
    canvas.drawText(TipoAvisoCalle.emoji(tipo), radio, y, texto)
    return BitmapDrawable(context.resources, bitmap)
}

/** Marcador de un aviso: al tocarlo muestra "🚓 Control" y "Mate de Luna 2400 · hace 5 min". */
fun markerAvisoCalle(map: MapView, context: Context, aviso: AvisoCalleDto): Marker =
    Marker(map).apply {
        position = GeoPoint(aviso.lat, aviso.lng)
        icon = pinAvisoCalle(context, aviso.tipo)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "${TipoAvisoCalle.emoji(aviso.tipo)} ${aviso.tipoTexto}"
        snippet = listOf(aviso.calle ?: AvisosCalleTexto.SIN_CALLE, AvisosCalleTexto.haceCuanto(aviso.creadoEn))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }
