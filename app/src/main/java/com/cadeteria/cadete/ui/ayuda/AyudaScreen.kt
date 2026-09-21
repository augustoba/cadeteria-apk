package com.cadeteria.cadete.ui.ayuda

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.ui.theme.CademOrange
import com.cadeteria.cadete.ui.theme.Gray500

/**
 * Pantalla de Ayuda (mejora 2026-09-16, accesible desde Perfil) — antes no había ningún
 * canal para "tengo una duda general" fuera del chat, que está pensado para coordinar un
 * viaje puntual, no para una consulta administrativa (ej. "¿cuándo me pagan?").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyudaScreen(onVolver: () -> Unit, onVerTutorial: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    var telefono by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        app.cadeteRepository.miConfiguracion().onSuccess { telefono = it.telefonoSoporte.ifBlank { null } }
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = CademOrange)
                    Text("¿Con qué te podemos ayudar?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Para coordinar un viaje puntual usá el chat de ese viaje. Para cualquier otra consulta " +
                            "(pagos, documentación, tu cuenta), comunicate directo con la cadetería.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500,
                    )
                }
            }

            OutlinedButton(onClick = onVerTutorial, modifier = Modifier.fillMaxWidth()) {
                Text("📖 Ver el tutorial de bienvenida de nuevo")
            }

            if (cargando) {
                CircularProgressIndicator()
            } else if (telefono == null) {
                Text(
                    "Todavía no hay un teléfono de contacto cargado — pedile al admin que lo complete en Configuración.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500,
                )
            } else {
                Button(
                    onClick = { llamar(context, telefono!!) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CademOrange),
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null)
                    Text(" Llamar a la cadetería", modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(onClick = { abrirWhatsapp(context, telefono!!) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Escribir por WhatsApp")
                }
            }
        }
    }
}

private fun llamar(context: Context, telefono: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")))
}

private fun abrirWhatsapp(context: Context, telefono: String) {
    val numero = telefono.filter { it.isDigit() }
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero")))
}
