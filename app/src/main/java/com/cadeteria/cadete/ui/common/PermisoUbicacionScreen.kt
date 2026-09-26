package com.cadeteria.cadete.ui.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.cadeteria.cadete.location.tienePermisoUbicacion
import com.cadeteria.cadete.ui.theme.CademOrange
import com.cadeteria.cadete.ui.theme.Gray700

/**
 * Bloquea toda la app, login incluido, mientras no haya permiso de ubicación precisa (2026-09-26).
 * Antes la app lo pedía al entrar pero no miraba la respuesta: con el permiso denegado el cadete se
 * ponía Libre, recibía viajes y no mandaba su posición. Mismo criterio que [UbicacionDesactivadaScreen].
 *
 * Si el cadete tocó "No permitir" dos veces, Android ya no vuelve a mostrar el cartel: ahí el botón
 * pasa a llevarlo directo a los permisos de la app en Ajustes.
 */
@Composable
fun PermisoUbicacionScreen(onConcedido: () -> Unit) {
    val context = LocalContext.current
    var yaPidio by rememberSaveable { mutableStateOf(false) }
    var bloqueadoPorAndroid by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        yaPidio = true
        if (tienePermisoUbicacion(context)) {
            onConcedido()
        } else {
            // Sin cartel de "por qué" después de pedirlo = Android ya no lo va a volver a mostrar.
            val activity = context.buscarActivity()
            bloqueadoPorAndroid = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val pedir = {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // La primera vez se pide solo, sin que tenga que tocar nada.
    LaunchedEffect(Unit) { if (!yaPidio) pedir() }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.LocationOff,
                contentDescription = null,
                tint = CademOrange,
                modifier = Modifier.height(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Necesitamos tu ubicación",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (bloqueadoPorAndroid) {
                    "Sin permiso de ubicación no podés recibir viajes. Entrá a Ajustes → Permisos → " +
                        "Ubicación y elegí \"Permitir mientras la app está en uso\", con \"Ubicación precisa\" activada."
                } else {
                    "Sin permiso de ubicación no podés recibir viajes. Cuando te lo pregunte, elegí " +
                        "\"Mientras la app está en uso\" y dejá activada la \"Ubicación precisa\"."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (bloqueadoPorAndroid) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    } else {
                        pedir()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (bloqueadoPorAndroid) "Abrir ajustes" else "Dar permiso")
            }
        }
    }
}

private fun Context.buscarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> null
}
