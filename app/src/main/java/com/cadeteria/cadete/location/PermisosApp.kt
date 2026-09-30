package com.cadeteria.cadete.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

/**
 * Los permisos sin los que no se usa la app (2026-09-29, pedido del usuario). La cámara no está: la app
 * abre la cámara del celular para las fotos y eso no necesita permiso.
 */
enum class PermisoApp(val titulo: String, val paraQue: String) {
    UBICACION("Ubicación precisa", "Para ofrecerte viajes cerca y que la central te vea en el mapa."),
    NOTIFICACIONES("Notificaciones", "Para enterarte de un viaje nuevo aunque tengas la app cerrada."),
    MICROFONO("Micrófono", "Para mandarle notas de voz a la central sin escribir, manejando."),
    BATERIA(
        "Batería sin restricciones",
        "Si no, Android cierra la app para ahorrar batería: se corta tu ubicación y no te llegan los viajes.",
    ),
}

object PermisosApp {

    /** Lógica pura (testeable): los que faltan, en el orden en que se muestran. */
    fun faltantes(ubicacion: Boolean, notificaciones: Boolean, microfono: Boolean, bateria: Boolean): List<PermisoApp> =
        buildList {
            if (!ubicacion) add(PermisoApp.UBICACION)
            if (!notificaciones) add(PermisoApp.NOTIFICACIONES)
            if (!microfono) add(PermisoApp.MICROFONO)
            if (!bateria) add(PermisoApp.BATERIA)
        }

    fun faltantes(context: Context): List<PermisoApp> = faltantes(
        ubicacion = tienePermisoUbicacion(context),
        // Para todas las versiones: antes de Android 13 no hay que pedirlo, pero se puede apagar en Ajustes.
        notificaciones = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        microfono = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        bateria = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName),
    )

    /** "NOTIFICACIONES,BATERIA": lo que se le informa al backend para la ficha del cadete. */
    fun paraInformar(faltantes: List<PermisoApp>): String = faltantes.joinToString(",") { it.name }
}

/**
 * Los que faltan, vueltos a mirar cada vez que la app vuelve a primer plano: se pueden sacar desde
 * Ajustes, o Android los borra solo ("Solo esta vez", apps sin usar).
 */
@Composable
fun rememberPermisosFaltantes(): MutableState<List<PermisoApp>> {
    val context = LocalContext.current
    val estado = remember { mutableStateOf(PermisosApp.faltantes(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) estado.value = PermisosApp.faltantes(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return estado
}
