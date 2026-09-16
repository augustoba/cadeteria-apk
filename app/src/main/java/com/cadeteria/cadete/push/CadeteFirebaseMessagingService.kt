package com.cadeteria.cadete.push

import android.util.Log
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.EventoViaje
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Respaldo de FCM cuando la app está en background/cerrada — mismo evento que llega
 * por WebSocket si la app está abierta (diseno-tecnico.md sección 4). El payload
 * "data" trae `tipo` (VIAJE_ASIGNADO/VIAJE_QUITADO/VIAJE_CANCELADO/CHAT) y, para
 * viajes, `pedidoId`. Si Firebase no está configurado (falta google-services.json,
 * ver README) esta clase simplemente nunca recibe nada — no hace falta guardarla.
 */
class CadeteFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val app = application as CadeteApp
        CoroutineScope(Dispatchers.IO).launch {
            app.cadeteRepository.actualizarFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val tipo = message.data["tipo"] ?: return
        val titulo = message.notification?.title ?: tituloPorDefecto(tipo)
        val cuerpo = message.notification?.body ?: ""

        val canal = if (tipo == "CHAT") NotificationHelper.CANAL_CHAT else NotificationHelper.CANAL_VIAJES
        val destino = when (tipo) {
            "CHAT" -> NotificationHelper.DESTINO_CHAT
            EventoViaje.VIAJE_ASIGNADO -> NotificationHelper.DESTINO_VIAJE
            else -> null
        }
        val pedidoId = if (tipo == EventoViaje.VIAJE_ASIGNADO) message.data["pedidoId"] else null
        NotificationHelper.mostrar(
            applicationContext, canal, id = tipo.hashCode(), titulo = titulo, cuerpo = cuerpo,
            destino = destino, pedidoId = pedidoId,
        )

        Log.d("CadeteFCM", "Push recibido: tipo=$tipo pedidoId=${message.data["pedidoId"]}")
    }

    private fun tituloPorDefecto(tipo: String): String = when (tipo) {
        EventoViaje.VIAJE_ASIGNADO -> "Nuevo viaje"
        EventoViaje.VIAJE_QUITADO -> "Viaje quitado"
        EventoViaje.VIAJE_CANCELADO -> "Viaje cancelado"
        "CHAT" -> "Nuevo mensaje"
        else -> "Cadetería"
    }
}
