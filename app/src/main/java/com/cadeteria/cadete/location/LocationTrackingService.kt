package com.cadeteria.cadete.location

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.R
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
    }

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
