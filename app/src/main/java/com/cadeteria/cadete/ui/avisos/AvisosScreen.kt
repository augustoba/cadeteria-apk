package com.cadeteria.cadete.ui.avisos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.AvisoHistorialDto
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.CargandoFullScreen
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.cadeteria.cadete.ui.theme.CademOrange
import com.cadeteria.cadete.ui.theme.Gray500

/** Pantalla "Avisos" con historial (mejora 2026-09-16, accesible desde Perfil) — antes un aviso general que llegaba por push/banner desaparecía para siempre en cuanto se marcaba leído. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisosScreen(onVolver: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: AvisosViewModel = viewModel(factory = ViewModelFactory(app) { AvisosViewModel(it) })
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avisos") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = vm::cargar) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar") }
                },
            )
        },
    ) { padding ->
        if (state.cargando) {
            CargandoFullScreen()
            return@Scaffold
        }
        if (state.error != null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                BannerError(state.error!!, Modifier.fillMaxWidth())
            }
            return@Scaffold
        }
        if (state.avisos.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Todavía no llegó ningún aviso general de la cadetería.", style = MaterialTheme.typography.bodyMedium, color = Gray500)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.avisos, key = { it.id }) { aviso -> AvisoCard(aviso) }
        }
    }
}

@Composable
private fun AvisoCard(aviso: AvisoHistorialDto) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Campaign, contentDescription = null, tint = CademOrange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatearFecha(aviso.enviadoEn), style = MaterialTheme.typography.labelMedium, color = Gray500)
                if (!aviso.leidoPorMi) {
                    Spacer(Modifier.weight(1f))
                    Text("● Nuevo", style = MaterialTheme.typography.labelMedium, color = CademOrange, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(aviso.mensaje, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** "2026-09-16T14:05:30" -> "16/09 14:05" — sin java.time (minSdk 24 sin desugaring, ver PerfilScreen). */
private fun formatearFecha(iso: String): String = runCatching {
    val fecha = iso.substringBefore("T")
    val hora = iso.substringAfter("T").take(5)
    val partes = fecha.split("-")
    "${partes[2]}/${partes[1]} $hora"
}.getOrDefault(iso)
