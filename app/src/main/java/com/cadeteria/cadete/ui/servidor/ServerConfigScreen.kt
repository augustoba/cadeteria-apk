package com.cadeteria.cadete.ui.servidor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.ui.common.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * Primera pantalla si nunca se configuró el backend — no hay Play Store ni distribución
 * centralizada (la APK se instala a mano por bluetooth), así que cada dispositivo tiene
 * que poder apuntar al servidor real sin recompilar la app.
 */
@Composable
fun ServerConfigScreen(onContinuar: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: ServerConfigViewModel = viewModel(factory = ViewModelFactory(app) { ServerConfigViewModel(it) })
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { url = vm.cargarActual() }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Configuración del servidor", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pedile la dirección al admin (ej: http://192.168.1.50:8080). Se puede " +
                    "cambiar después desde el perfil.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL del backend") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { scope.launch { vm.guardar(url); onContinuar() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank(),
            ) {
                Text("Continuar")
            }
        }
    }
}
