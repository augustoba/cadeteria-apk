package com.cadeteria.cadete.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.VotoAvisoCalleRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Botones "Sigue" / "Ya no está" de la notificación "¿Sigue ahí?" (avisos de la calle, segunda etapa,
 * 2026-09-29). Manda la respuesta sin abrir la app. Sin señal se pierde: un voto de hace un rato ya
 * no sirve (misma idea que el aviso, que tampoco se encola).
 */
class VotoAvisoCalleReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_AVISO_ID = "avisoId"
        const val EXTRA_VOTO = "voto"
        const val SIGUE = "SIGUE"
        const val YA_NO_ESTA = "YA_NO_ESTA"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val avisoId = intent.getStringExtra(EXTRA_AVISO_ID) ?: return
        val voto = intent.getStringExtra(EXTRA_VOTO) ?: return
        NotificationHelper.cancelar(context, NotificationHelper.idSigueAhi(avisoId))
        val app = context.applicationContext as CadeteApp
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { app.retrofitProvider.apiService().votarAvisoCalle(avisoId, VotoAvisoCalleRequest(voto)) }
                    .onSuccess { app.avisosCalleStore.recibir(it) }
            } finally {
                pendiente.finish()
            }
        }
    }
}
