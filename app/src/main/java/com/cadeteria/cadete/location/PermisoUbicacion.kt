package com.cadeteria.cadete.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Permiso de ubicación PRECISA para la app (2026-09-26). Con solo la aproximada (Android 12+,
 * "Ubicación aproximada") el error es de kilómetros: no sirve ni para el mapa ni para aprender calles.
 */
fun tienePermisoUbicacion(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Igual que [rememberUbicacionHabilitada] pero para el permiso: se vuelve a mirar al volver a primer
 * plano, porque se puede sacar desde Ajustes, o Android lo borra solo si el cadete eligió "Solo esta vez".
 */
@Composable
fun rememberPermisoUbicacion(): MutableState<Boolean> {
    val context = LocalContext.current
    val estado = remember { mutableStateOf(tienePermisoUbicacion(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) estado.value = tienePermisoUbicacion(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return estado
}
