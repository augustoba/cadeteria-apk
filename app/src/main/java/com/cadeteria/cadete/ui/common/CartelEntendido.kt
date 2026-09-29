package com.cadeteria.cadete.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Cartel que se cierra solo con "Entendido" (2026-09-29): recordatorios al entrar y avisos generales
 * del admin. No se cierra tocando afuera ni con "atrás", ni solo por tiempo. Si el texto no entra se
 * desliza con el dedo y el botón queda siempre visible abajo (el AlertDialog le da al texto el lugar
 * que sobra).
 *
 * @param vinetas true = cada renglón con "•" (recordatorios); false = un texto corrido (aviso).
 */
@Composable
fun CartelEntendido(
    titulo: String,
    renglones: List<String>,
    onEntendido: () -> Unit,
    vinetas: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(titulo) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                renglones.forEach { Text(if (vinetas) "• $it" else it) }
            }
        },
        confirmButton = { Button(onClick = onEntendido) { Text("Entendido") } },
    )
}
