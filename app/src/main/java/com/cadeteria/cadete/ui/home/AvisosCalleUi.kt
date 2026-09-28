package com.cadeteria.cadete.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.data.remote.dto.TipoAvisoCalle
import com.cadeteria.cadete.ui.theme.Gray500
import com.cadeteria.cadete.ui.theme.Red600

/**
 * "Avisos de la calle" en Inicio (carril C, 2026-09-28): botón "🚨 Avisar" con 4 opciones grandes de
 * un solo toque (sin escribir nada) y la lista "Avisos cerca tuyo". Una línea por aviso: se lee
 * manejando.
 */
@Composable
fun AvisosCalleSeccion(
    avisos: List<AvisoCalleDto>,
    enviando: Boolean,
    mensaje: String?,
    onAvisar: (String) -> Unit,
    onCerrarMensaje: () -> Unit,
) {
    var elegir by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { elegir = true },
            enabled = !enviando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red600),
        ) {
            if (enviando) {
                CircularProgressIndicator(Modifier.height(20.dp))
                Text("  Mandando aviso…")
            } else {
                Text("🚨 Avisar algo de la calle", fontWeight = FontWeight.SemiBold)
            }
        }

        if (avisos.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Avisos cerca tuyo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    avisos.filter { AvisosCalleTexto.vigente(it) }.forEach {
                        Text(AvisosCalleTexto.linea(it), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (elegir) {
        AlertDialog(
            onDismissRequest = { elegir = false },
            title = { Text("¿Qué pasa en la calle?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TipoAvisoCalle.OPCIONES.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fila.forEach { (tipo, emoji, texto) ->
                                Button(
                                    onClick = {
                                        elegir = false
                                        onAvisar(tipo)
                                    },
                                    modifier = Modifier.weight(1f).height(84.dp),
                                ) {
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                        Text(emoji, fontSize = 26.sp)
                                        Text(texto, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Les llega a los cadetes que andan cerca. Queda registrado quién avisa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500,
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { elegir = false }) { Text("Cancelar") } },
        )
    }

    mensaje?.let {
        AlertDialog(
            onDismissRequest = onCerrarMensaje,
            text = { Text(it) },
            confirmButton = { TextButton(onClick = onCerrarMensaje) { Text("Listo") } },
        )
    }
}
