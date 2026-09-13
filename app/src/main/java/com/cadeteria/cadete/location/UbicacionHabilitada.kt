package com.cadeteria.cadete.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** El switch de ubicación del sistema (no el permiso) — GPS/red apagados a nivel Android. */
fun ubicacionHabilitada(context: Context): Boolean {
    val manager = context.getSystemService<LocationManager>() ?: return false
    return LocationManagerCompat.isLocationEnabled(manager)
}

/**
 * Spec: si el cadete apaga la ubicación del sistema, la app se bloquea hasta que la
 * vuelva a prender (ver [com.cadeteria.cadete.ui.common.UbicacionDesactivadaScreen]).
 * Se refresca con el broadcast del sistema (apagar/prender GPS con la app abierta) y al
 * volver a primer plano (por si el cambio se hizo desde Ajustes con la app en background).
 */
@Composable
fun rememberUbicacionHabilitada(): State<Boolean> {
    val context = LocalContext.current
    val estado = remember { mutableStateOf(ubicacionHabilitada(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                estado.value = ubicacionHabilitada(context)
            }
        }
        context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        onDispose { context.unregisterReceiver(receiver) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) estado.value = ubicacionHabilitada(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return estado
}
