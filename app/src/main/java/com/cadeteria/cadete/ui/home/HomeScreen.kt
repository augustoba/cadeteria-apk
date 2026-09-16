package com.cadeteria.cadete.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.EstadoCadete
import com.cadeteria.cadete.data.remote.dto.EstadoPedido
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.location.LocationServiceController
import com.cadeteria.cadete.ui.common.AppScaffold
import com.cadeteria.cadete.ui.common.AvisoFlotante
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.CargandoFullScreen
import com.cadeteria.cadete.ui.common.ContadorAceptacion
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.cadeteria.cadete.ui.navigation.Routes
import com.cadeteria.cadete.ui.theme.Amber500
import com.cadeteria.cadete.ui.theme.Emerald600
import com.cadeteria.cadete.ui.theme.Gray500
import com.cadeteria.cadete.ui.theme.Red600

private val permisosUbicacion = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAbrirViaje: (String) -> Unit,
    onAbrirChat: () -> Unit,
    onIrHistorial: () -> Unit,
    onIrPerfil: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: HomeViewModel = viewModel(factory = ViewModelFactory(app) { HomeViewModel(it) })
    val state by vm.uiState.collectAsState()

    val permisosLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(Unit) { permisosLauncher.launch(permisosUbicacion.toTypedArray()) }

    state.bienvenida?.let { b ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = vm::cerrarBienvenida,
            title = { Text("¡Bienvenido, ${b.nombre}!") },
            text = {
                Column {
                    Text("Tu saldo disponible es:")
                    Text(
                        "$${"%.2f".format(b.saldo)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (b.saldoBajo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    if (b.saldoBajo) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tu saldo está bajo — puede que no te alcance para aceptar el próximo viaje. Cargá crédito cuando puedas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = { Button(onClick = vm::cerrarBienvenida) { Text("Entendido") } },
        )
    }

    if (state.mostrarRecordatorios) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = vm::cerrarRecordatorios,
            title = { Text("Antes de arrancar") },
            text = {
                Column {
                    Text("• Llevá toda la documentación en regla (DNI, licencia, cédula del vehículo, seguro).")
                    Spacer(Modifier.height(8.dp))
                    Text("• No te olvides los elementos de seguridad: casco, cadena y mochila.")
                    Spacer(Modifier.height(8.dp))
                    Text("• Marcá cada viaje como \"Retirado\" al levantar el pedido, y \"Finalizado\" con los datos correspondientes al entregarlo.")
                }
            },
            confirmButton = { Button(onClick = vm::cerrarRecordatorios) { Text("Entendido") } },
        )
    }

    if (state.documentacionFaltante.isNotEmpty()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = vm::cerrarDialogoDocumentacion,
            title = { Text("Te falta cargar documentación") },
            text = {
                Column {
                    Text("Antes de activarte, pedile al admin que te ayude a cargar:")
                    Spacer(Modifier.height(8.dp))
                    state.documentacionFaltante.forEach { Text("• $it") }
                }
            },
            confirmButton = { Button(onClick = vm::cerrarDialogoDocumentacion) { Text("Entendido") } },
        )
    }

    AppScaffold(
        title = "Dashboard",
        currentRoute = Routes.HOME,
        cadeteNombre = state.cadete?.nombre,
        onIrDashboard = {},
        onIrHistorial = onIrHistorial,
        onIrPerfil = onIrPerfil,
        onCerrarSesion = onCerrarSesion,
        actions = {
            val chatNoLeidos by app.chatNoLeidos.collectAsState()
            IconButton(onClick = onAbrirChat) {
                BadgedBox(badge = {
                    if (chatNoLeidos > 0) Badge { Text(if (chatNoLeidos > 9) "9+" else "$chatNoLeidos") }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat")
                }
            }
            IconButton(onClick = vm::cargar) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar") }
        },
    ) { padding ->
        if (state.cargando && state.cadete == null) {
            CargandoFullScreen()
            return@AppScaffold
        }

        val pullState = rememberPullToRefreshState()
        LaunchedEffect(pullState.isRefreshing) {
            if (pullState.isRefreshing) vm.cargar()
        }
        LaunchedEffect(state.cargando) {
            if (!state.cargando) pullState.endRefresh()
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullState.nestedScrollConnection),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                state.error?.let {
                    BannerError(it, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }

                EstadoCard(
                    estadoId = state.cadete?.estado?.id ?: EstadoCadete.DESCONECTADO,
                    cambiando = state.cambiandoEstado,
                    onToggle = {
                        vm.toggleDisponibilidad(
                            onLocationServiceStart = { LocationServiceController.iniciar(context) },
                            onLocationServiceStop = { LocationServiceController.detener(context) },
                        )
                    },
                    onToggleOcupado = {
                        vm.toggleOcupado(
                            onLocationServiceStart = { LocationServiceController.iniciar(context) },
                            onLocationServiceStop = { LocationServiceController.detener(context) },
                        )
                    },
                )

                Spacer(Modifier.height(24.dp))
                Text(
                    "Asignados y en curso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))

                if (state.activos.isEmpty()) {
                    EstadoVacio()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (viaje in state.activos) {
                            ViajeResumenCard(
                                viaje,
                                tiempoLimiteAceptacionSeg = state.tiempoLimiteAceptacionSeg,
                                onVerDetalle = { onAbrirViaje(viaje.id) },
                            )
                        }
                    }
                }
            }

            PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))

            state.avisoFlotante?.let { mensaje ->
                AvisoFlotante(
                    mensaje = mensaje,
                    onCerrar = vm::cerrarAvisoFlotante,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun EstadoCard(estadoId: String, cambiando: Boolean, onToggle: () -> Unit, onToggleOcupado: () -> Unit) {
    val (texto, subtitulo, color) = when (estadoId) {
        EstadoCadete.LIBRE -> Triple("Estás LIBRE", "Podés recibir viajes nuevos.", Emerald600)
        EstadoCadete.OCUPADO -> Triple("Estás OCUPADO", "No te van a asignar viajes nuevos hasta que te pongas libre de nuevo.", Amber500)
        else -> Triple("Estás DESCONECTADO", "Activate para empezar a recibir viajes.", Red600)
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, tint = color)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = color)
                    Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = Gray500)
                }
            }
            Spacer(Modifier.height(16.dp))
            // Botón grande en vez de un switch chico (auditoría UX 2026-09-13): es la
            // acción más importante del día, tiene que poder tocarse rápido y sin
            // puntería, apurado o con guantes.
            val esDesconectado = estadoId == EstadoCadete.DESCONECTADO
            Button(
                onClick = onToggle,
                enabled = !cambiando,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (esDesconectado) Emerald600 else Red600),
            ) {
                if (cambiando) {
                    CircularProgressIndicator(Modifier.height(24.dp), color = Color.White)
                } else {
                    Text(
                        if (esDesconectado) "🟢 Activarme" else "🔴 Desconectarme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
            }
            if (!esDesconectado) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onToggleOcupado,
                    enabled = !cambiando,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(
                        if (estadoId == EstadoCadete.OCUPADO) "▶ Ponerme libre" else "⏸ Ponerme ocupado (no asignarme más)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoVacio() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Moto + bici en vez del camión genérico de antes — son los vehículos con los que
        // realmente reparten los cadetes.
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = Gray500, modifier = Modifier.size(40.dp))
            Icon(Icons.Filled.PedalBike, contentDescription = null, tint = Gray500, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "No tenés ningún viaje asignado por ahora.",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
        )
    }
}

@Composable
private fun ViajeResumenCard(viaje: PedidoDto, tiempoLimiteAceptacionSeg: Int, onVerDetalle: () -> Unit) {
    val esPendiente = viaje.estado.id == EstadoPedido.PENDIENTE
    val color = if (esPendiente) Amber500 else Emerald600
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color),
            )
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (esPendiente) Icons.Filled.NotificationsActive else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (esPendiente) "¡Viaje nuevo! Respondé pronto" else "Viaje en curso",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = color,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tenés asignado el pedido #${viaje.numero}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (esPendiente) {
                    Spacer(Modifier.height(4.dp))
                    ContadorAceptacion(viaje.asignadoEn, tiempoLimiteAceptacionSeg)
                }
                if (!esPendiente) {
                    // Ya aceptado: si el cadete tiene varios en curso, necesita el
                    // origen/destino acá para distinguirlos sin entrar al detalle.
                    Text("Desde: ${viaje.origenDireccion}", style = MaterialTheme.typography.bodyMedium)
                    Text("Hasta: ${viaje.destinoDireccion}", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onVerDetalle,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = if (esPendiente) ButtonDefaults.buttonColors(containerColor = Amber500) else ButtonDefaults.buttonColors(),
                ) {
                    Text(
                        if (esPendiente) "Ver y responder" else "Ver viaje",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
