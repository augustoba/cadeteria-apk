package com.cadeteria.cadete.ui.common

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.cadeteria.cadete.location.PermisoApp
import com.cadeteria.cadete.location.PermisosApp
import com.cadeteria.cadete.ui.theme.Emerald600
import com.cadeteria.cadete.ui.theme.Gray500

/**
 * Permisos necesarios (2026-09-29, reemplaza a la pantalla que pedía solo la ubicación): bloquea toda la
 * app, login incluido, hasta tener ubicación precisa, notificaciones, micrófono y batería sin
 * restricciones. Cada uno con su tilde o su botón. Si Android ya no deja volver a preguntar (lo negó
 * dos veces), el botón lleva a los Ajustes de la app. [alCambiar] vuelve a mirar qué falta.
 */
@Composable
fun PermisosScreen(faltantes: List<PermisoApp>, alCambiar: () -> Unit) {
    val context = LocalContext.current
    // Permisos que Android ya no deja volver a pedir: el botón pasa a abrir Ajustes.
    var bloqueados by rememberSaveable { mutableStateOf(setOf<String>()) }
    var primeraVez by rememberSaveable { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        val activity = context.buscarActivity()
        bloqueados = bloqueados + resultado.filterValues { !it }.keys.filter { permiso ->
            activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permiso)
        }
        alCambiar()
    }

    // La primera vez se piden solos los del cartel de Android (ubicación, notificaciones, micrófono).
    LaunchedEffect(Unit) {
        if (primeraVez) {
            primeraVez = false
            val aPedir = faltantes.flatMap { permisosDeAndroid(it) }
            if (aPedir.isNotEmpty()) launcher.launch(aPedir.toTypedArray())
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            LogoCadem(tamano = 96.dp)
            Spacer(Modifier.height(16.dp))
            Text("Permisos necesarios", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Sin estos permisos la app no puede funcionar bien. Dalos una sola vez y listo.",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            PermisoApp.values().forEach { permiso ->
                FilaPermiso(
                    permiso = permiso,
                    concedido = permiso !in faltantes,
                    onDar = {
                        val deAndroid = permisosDeAndroid(permiso)
                        when {
                            permiso == PermisoApp.BATERIA -> abrirBateria(context)
                            deAndroid.isEmpty() || deAndroid.any { it in bloqueados } -> abrirAjustesDe(context, permiso)
                            else -> launcher.launch(deAndroid.toTypedArray())
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun FilaPermiso(permiso: PermisoApp, concedido: Boolean, onDar: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(permiso.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(permiso.paraQue, style = MaterialTheme.typography.bodySmall, color = Gray500)
            }
            Spacer(Modifier.width(12.dp))
            if (concedido) {
                Box(Modifier.size(36.dp).background(Emerald600, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = "Dado", tint = Color.White)
                }
            } else {
                Button(onClick = onDar, modifier = Modifier.efectoToque()) { Text("Dar permiso") }
            }
        }
    }
}

/** Los del cartel de Android para cada uno (vacío = no hay cartel: se va a Ajustes). */
private fun permisosDeAndroid(permiso: PermisoApp): List<String> = when (permiso) {
    PermisoApp.UBICACION -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    PermisoApp.NOTIFICACIONES -> if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
    PermisoApp.MICROFONO -> listOf(Manifest.permission.RECORD_AUDIO)
    PermisoApp.BATERIA -> emptyList()
}

private fun abrirAjustesDe(context: Context, permiso: PermisoApp) {
    val intent = if (permiso == PermisoApp.NOTIFICACIONES && Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    runCatching { context.startActivity(intent) }
}

/** El cartel de Android "¿Permitir que la app se ejecute en segundo plano?"; si el celular no lo tiene, la lista. */
@SuppressLint("BatteryLife") // la app se reparte por fuera de Play Store: no aplica su política
private fun abrirBateria(context: Context) {
    val directo = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(directo) }
        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } }
}

private fun Context.buscarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> null
}
