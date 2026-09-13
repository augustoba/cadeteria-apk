package com.cadeteria.cadete.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cadeteria.cadete.ui.theme.CademOrange
import kotlinx.coroutines.delay

@Composable
fun CargandoFullScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun BannerError(mensaje: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(mensaje, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Aviso general del admin, flotando arriba de la pantalla — a diferencia de la
 * notificación del sistema (que desaparece sola y no deja rastro), esto se queda a la
 * vista hasta que el cadete lo cierra o pasan unos segundos. Se muestra encima del
 * contenido (usar dentro de un Box con Alignment.TopCenter), no en el flujo normal.
 */
@Composable
fun AvisoFlotante(mensaje: String, onCerrar: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(mensaje) {
        delay(8_000)
        onCerrar()
    }
    Card(
        modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Campaign, contentDescription = null, tint = CademOrange)
            Text(
                mensaje,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            IconButton(onClick = onCerrar) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
            }
        }
    }
}

@Composable
fun BannerInfo(mensaje: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(mensaje, color = Color(0xFF1E40AF), style = MaterialTheme.typography.bodyMedium)
    }
}
