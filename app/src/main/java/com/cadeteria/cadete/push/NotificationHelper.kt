package com.cadeteria.cadete.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cadeteria.cadete.MainActivity
import com.cadeteria.cadete.R

/**
 * Canales y construcción de notificaciones — usado tanto por el push (FCM) como por
 * el foreground service de ubicación y los avisos por WebSocket (RealtimeManager).
 * Vibración y sonido al máximo a propósito (spec: "que vibren y suenen lo más fuerte
 * posible") — un cadete en la calle con el celular en el bolsillo se puede perder un
 * aviso más sutil.
 *
 * OJO Android 8+: una vez creado, un canal no puede cambiar sonido/vibración/importancia
 * por código — solo el usuario puede tocarlo desde Ajustes. Si esto se ajusta después de
 * que la app ya esté instalada en los celus de los cadetes, hace falta que cada uno entre
 * a Ajustes > Notificaciones y lo habilite a mano, o borrar y reinstalar la app.
 */
object NotificationHelper {

    // Sufijo "_v2": un canal no puede cambiar de sonido por código una vez creado (ver nota
    // abajo), así que para que el sonido más largo llegue a los celus que ya tenían la app
    // instalada con el canal viejo, hace falta un ID nuevo — Android lo crea de cero.
    const val CANAL_VIAJES = "viajes_v2"
    const val CANAL_CHAT = "chat_v2"
    const val CANAL_UBICACION = "ubicacion"
    /** Recordatorio cada 30 min en DESCONECTADO/OCUPADO — sonido normal, no el ringtone de los viajes. */
    const val CANAL_RECORDATORIOS = "recordatorios"
    /**
     * "Llegaste al retiro / a la entrega, no te olvides de marcarlo" (2026-09-26, ver
     * location/AvisoLlegada): importancia alta con el sonido normal de notificación — tiene que
     * notarse con el celular en el bolsillo, pero no es un viaje nuevo (no el ringtone largo).
     */
    const val CANAL_LLEGADAS = "llegadas"

    private val PATRON_VIBRACION = longArrayOf(0, 500, 250, 500, 250, 500)

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        // TYPE_RINGTONE en vez de TYPE_NOTIFICATION: los tonos de llamada del teléfono son
        // bastante más largos que los chime cortos de notificación (que en varios Motorola
        // son casi un solo "tin"), y usan el mismo volumen de ringtone ya seteado arriba.
        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val viajes = NotificationChannel(CANAL_VIAJES, "Viajes y avisos", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = PATRON_VIBRACION
            enableLights(true)
            setSound(sonido, audioAttrs)
        }
        val chat = NotificationChannel(CANAL_CHAT, "Chat", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = PATRON_VIBRACION
            setSound(sonido, audioAttrs)
        }
        val ubicacion = NotificationChannel(CANAL_UBICACION, "Ubicación en vivo", NotificationManager.IMPORTANCE_LOW)

        manager.createNotificationChannel(viajes)
        manager.createNotificationChannel(chat)
        val recordatorios = NotificationChannel(
            CANAL_RECORDATORIOS, "Recordatorios de estado", NotificationManager.IMPORTANCE_DEFAULT,
        )

        manager.createNotificationChannel(ubicacion)
        manager.createNotificationChannel(recordatorios)
        val llegadas = NotificationChannel(CANAL_LLEGADAS, "Llegada al retiro o entrega", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Te recuerda marcar Retirado o Entregado cuando llegás a la dirección."
            enableVibration(true)
            vibrationPattern = PATRON_VIBRACION
        }
        manager.createNotificationChannel(llegadas)
    }

    fun cancelar(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    const val EXTRA_DESTINO = "destino"
    const val DESTINO_CHAT = "CHAT"
    /** Nuevo viaje asignado: tocar la notificación abre directo el detalle del pedido en vez
     * del Dashboard (auditoría UX 2026-09-15) — con el tiempo límite para aceptar corriendo,
     * cada toque de más cuenta. */
    const val DESTINO_VIAJE = "VIAJE"
    const val EXTRA_PEDIDO_ID = "pedidoId"

    fun mostrar(
        context: Context, canal: String, id: Int, titulo: String, cuerpo: String,
        destino: String? = null, pedidoId: String? = null,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (destino != null) putExtra(EXTRA_DESTINO, destino)
            if (pedidoId != null) putExtra(EXTRA_PEDIDO_ID, pedidoId)
        }
        // requestCode = id (no una constante fija): si no, todas las notificaciones activas
        // comparten el mismo PendingIntent y tocar una vieja terminaba abriendo el destino
        // de la última notificación mostrada, no el suyo.
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, id, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            // Ignorado en Android 8+ (manda el canal) — pero necesario en API 24/25
            // (Galaxy J7 y similares, el piso de compatibilidad del spec).
            .setVibrate(PATRON_VIBRACION)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .build()
        NotificationManagerCompat.from(context).notify(id, notif)
    }
}
