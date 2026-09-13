package com.cadeteria.cadete.location

import android.content.Context
import android.content.Intent
import android.os.Build

object LocationServiceController {
    fun iniciar(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun detener(context: Context) {
        context.stopService(Intent(context, LocationTrackingService::class.java))
    }
}
