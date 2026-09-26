package com.cadeteria.cadete.location

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.R
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.push.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manda la posición del cadete cada `frecuencia_ubicacion_seg` (configurable desde
 * el panel, spec 5.1/diseno sección 2/7) mientras la app esté "activada" — corre como
 * foreground service para que Android no la mate en segundo plano (obligatorio desde
 * Android 8, y el celular tiene que tener la ubicación siempre prendida per spec 3).
 */
class LocationTrackingService : Service() {

    companion object {
        const val NOTIF_ID = 1001
        private const val DEFAULT_INTERVAL_MS = 45_000L
        /** Cada cuánto se vuelven a pedir los viajes activos para el aviso de llegada. */
        private const val VIAJES_VIGENCIA_MS = 60_000L

        private fun idNotificacionLlegada(clave: String) = ("llegada:$clave").hashCode()
    }

    private val avisoLlegada = AvisoLlegada()
    private val mutexLlegada = Mutex()
    private var viajes: List<PedidoDto> = emptyList()
    private var viajesLeidosEn = 0L

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var fusedClient: FusedLocationProviderClient
    private var callback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, construirNotificacion())
        scope.launch { iniciarActualizaciones() }
        return START_STICKY
    }

    private suspend fun iniciarActualizaciones() {
        val app = application as CadeteApp
        val intervaloMs = app.cadeteRepository.miConfiguracion()
            .getOrNull()?.let { it.frecuenciaUbicacionSeg * 1000L } ?: DEFAULT_INTERVAL_MS

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervaloMs)
            .setMinUpdateIntervalMillis(intervaloMs / 2)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                scope.launch {
                    app.cadeteRepository.actualizarUbicacion(loc.latitude, loc.longitude)
                    revisarLlegada(app, loc.latitude, loc.longitude, if (loc.hasAccuracy()) loc.accuracy else null)
                }
            }
        }
        try {
            fusedClient.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // El permiso se pide desde HomeScreen antes de arrancar el service; si de
            // todas formas falta, no hay mucho más que hacer que dejar de intentar.
            stopSelf()
        }
    }

    /**
     * "Llegaste al retiro / a la entrega, no te olvides de marcarlo" (2026-09-26). Los viajes se
     * releen como mucho una vez por minuto, y otra vez justo antes de avisar: si el cadete ya
     * marcó Retirado hace unos segundos, el aviso no sale.
     */
    private suspend fun revisarLlegada(app: CadeteApp, lat: Double, lng: Double, precisionM: Float?) = mutexLlegada.withLock {
        val ahora = System.currentTimeMillis()
        if (ahora - viajesLeidosEn > VIAJES_VIGENCIA_MS) leerViajes(app, ahora)

        var pendientes = avisoLlegada.puntosPendientes(viajes)
        var avisos = avisoLlegada.procesar(pendientes, lat, lng, precisionM, ahora)
        if (avisos.isNotEmpty() && leerViajes(app, ahora)) {
            pendientes = avisoLlegada.puntosPendientes(viajes)
            val vigentes = pendientes.map { it.clave }.toSet()
            avisos = avisos.filter { it.punto.clave in vigentes }
        }
        avisos.forEach {
            NotificationHelper.mostrar(
                this, NotificationHelper.CANAL_LLEGADAS, idNotificacionLlegada(it.punto.clave),
                it.titulo, it.cuerpo,
                destino = NotificationHelper.DESTINO_VIAJE, pedidoId = it.punto.pedidoId,
            )
        }
        // Ya lo marcó (o se lo reasignaron): el recordatorio que quedó en la barra ya no sirve.
        avisoLlegada.clavesResueltas(pendientes).forEach {
            NotificationHelper.cancelar(this, idNotificacionLlegada(it))
        }
    }

    /** Sin conexión se sigue con la última lista (y se reintenta en el próximo ping). */
    private suspend fun leerViajes(app: CadeteApp, ahora: Long): Boolean {
        val leidos = app.pedidoRepository.viajesActivos().getOrNull() ?: return false
        viajes = leidos
        viajesLeidosEn = ahora
        return true
    }

    private fun construirNotificacion() =
        NotificationCompat.Builder(this, NotificationHelper.CANAL_UBICACION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Cadetería")
            .setContentText("Compartiendo tu ubicación mientras estás activo")
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
