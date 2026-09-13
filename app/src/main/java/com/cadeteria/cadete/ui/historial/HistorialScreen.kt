package com.cadeteria.cadete.ui.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.ui.common.AppScaffold
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.CargandoFullScreen
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.cadeteria.cadete.ui.navigation.Routes
import com.cadeteria.cadete.ui.theme.CademOrange
import com.cadeteria.cadete.ui.theme.Emerald600
import com.cadeteria.cadete.ui.theme.Gray500

/** Cuántos viajes finalizados se muestran de entrada — "Mostrar más" va sumando de a esto. */
private const val TAMANO_PAGINA = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onAbrirViaje: (String) -> Unit, onIrDashboard: () -> Unit, onIrPerfil: () -> Unit, onCerrarSesion: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: HistorialViewModel = viewModel(factory = ViewModelFactory(app) { HistorialViewModel(it) })
    val state by vm.uiState.collectAsState()
    var cantidadVisible by remember { mutableIntStateOf(TAMANO_PAGINA) }

    AppScaffold(
        title = "Historial",
        currentRoute = Routes.HISTORIAL,
        cadeteNombre = null,
        onIrDashboard = onIrDashboard,
        onIrHistorial = {},
        onIrPerfil = onIrPerfil,
        onCerrarSesion = onCerrarSesion,
        actions = {
            IconButton(onClick = vm::cargar) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar") }
        },
    ) { padding ->
        if (state.cargando) {
            CargandoFullScreen()
            return@AppScaffold
        }

        val h = state.historial
        if (h == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                BannerError(state.error ?: "No se pudo cargar el historial.", Modifier.fillMaxWidth())
            }
            return@AppScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangoChip("Hoy", state.rango == RangoHistorial.HOY) { vm.cambiarRango(RangoHistorial.HOY) }
                    RangoChip("Esta semana", state.rango == RangoHistorial.SEMANA) { vm.cambiarRango(RangoHistorial.SEMANA) }
                    RangoChip("Todo", state.rango == RangoHistorial.TODO) { vm.cambiarRango(RangoHistorial.TODO) }
                }
            }
            item {
                ResumenCard(state.rango, h.cantidadViajes, h.montoTotal, h.cantidadRechazados, h.cantidadNoAceptados)
            }
            item {
                Text(
                    "Viajes finalizados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (h.pedidos.isEmpty()) {
                item {
                    Text(
                        "Todavía no finalizaste ningún viaje.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500,
                    )
                }
            } else {
                val visibles = h.pedidos.take(cantidadVisible)
                items(visibles, key = { it.id }) { pedido ->
                    PedidoFinalizadoCard(pedido, onClick = { onAbrirViaje(pedido.id) })
                }
                if (cantidadVisible < h.pedidos.size) {
                    item {
                        OutlinedButton(
                            onClick = { cantidadVisible += TAMANO_PAGINA },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Mostrar más (${h.pedidos.size - cantidadVisible} más)") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangoChip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CademOrange, selectedLabelColor = Color.White),
    )
}

@Composable
private fun ResumenCard(rango: RangoHistorial, cantidadViajes: Int, montoTotal: Double, rechazados: Long, noAceptados: Long) {
    val titulo = when (rango) {
        RangoHistorial.HOY -> "Resumen de hoy"
        RangoHistorial.SEMANA -> "Resumen de los últimos 7 días"
        RangoHistorial.TODO -> "Resumen histórico"
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = CademOrange),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("$${"%.2f".format(montoTotal)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
            Text("$cantidadViajes viajes finalizados", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat("Rechazaste", rechazados.toString())
                MiniStat("No aceptaste a tiempo", noAceptados.toString())
            }
        }
    }
}

@Composable
private fun MiniStat(etiqueta: String, valor: String) {
    Column {
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
    }
}

/** "2026-09-12T14:05:30" -> "14:05"; "--" si todavía no pasó (ej. no se marcó retiro). */
private fun horaCorta(iso: String?): String = if (iso.isNullOrBlank() || iso.length < 16) "--" else iso.substring(11, 16)

@Composable
private fun PedidoFinalizadoCard(pedido: PedidoDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${pedido.origenDireccion} → ${pedido.destinoDireccion}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Aceptado ${horaCorta(pedido.aceptadoEn)} · Retirado ${horaCorta(pedido.retiradoEn)} · Entregado ${horaCorta(pedido.finalizadoEn)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                )
            }
            Text("$${pedido.precio}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CademOrange)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Gray500, modifier = Modifier.size(14.dp))
        }
    }
}
