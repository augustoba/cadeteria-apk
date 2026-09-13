package com.cadeteria.cadete.ui.common

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadeteria.cadete.ui.theme.CademOrange
import com.cadeteria.cadete.ui.theme.Gray700

/**
 * Bloquea toda la app (spec: "que no la deje abrir ni hacer nada") mientras la ubicación
 * del sistema esté apagada — un cadete sin ubicación no se puede rastrear ni asignar
 * viajes con confianza. Se muestra en vez del NavGraph entero, no solo en una pantalla.
 */
@Composable
fun UbicacionDesactivadaScreen(onActivarUbicacion: () -> Unit) {
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
                "Ubicación desactivada",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Para usar la app necesitás tener la ubicación del teléfono activada. " +
                    "Activala para continuar.",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onActivarUbicacion, modifier = Modifier.fillMaxWidth()) {
                Text("Activar ubicación")
            }
        }
    }
}
