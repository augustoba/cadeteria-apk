package com.cadeteria.cadete.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TwoWheeler
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.cadeteria.cadete.ui.common.RutaRetiroEntrega
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.cadeteria.cadete.ui.common.formatearPesos
import com.cadeteria.cadete.ui.navigation.Routes
import com.cadeteria.cadete.ui.theme.Amber500
import com.cadeteria.cadete.ui.theme.Emerald600
import com.cadeteria.cadete.ui.theme.EstiloNumero
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
    onIrAyuda: () -> Unit,
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
        title = "Inicio",
        currentRoute = Routes.HOME,
        cadeteNombre = state.cadete?.nombre,
        onIrDashboard = {},
        onIrHistorial = onIrHistorial,
        onIrPerfil = onIrPerfil,
        onCerrarSesion = onCerrarSesion,
        onIrChat = onAbrirChat,
        onIrAyuda = onIrAyuda,
        actions = {
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

                Spacer(Modifier.height(12.dp))
                EstadisticasDeHoy(
                    viajes = state.viajesHoy,
                    facturado = state.facturadoHoy,
                    minutosConectado = state.minutosConectadoHoy,
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
                        state.activos.forEachIndexed { indice, viaje ->
                            key(viaje.id) {
                                EntradaAnimada(retrasoMs = indice * 60) {
                                    ViajeResumenCard(
                                        viaje,
                                        tiempoLimiteAceptacionSeg = state.tiempoLimiteAceptacionSeg,
                                        onVerDetalle = { onAbrirViaje(viaje.id) },
                                    )
                                }
                            }
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
        colors = CardDefaults.cardColors(containerColor = fondoDeEstado(estadoId, color)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    if (estadoId == EstadoCadete.LIBRE) {
                        PulsoLibre(color, Modifier.size(44.dp))
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(color.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PowerSettingsNew, contentDescription = null, tint = color)
                    }
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
                        if (estadoId == EstadoCadete.OCUPADO) "▶ Ponerme libre" else "⏸ Ponerme ocupado",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * Fondo de la card de estado (spec mejoras visuales §2, cambio 1): verde / ámbar / rojo suave
 * según el estado, para que se entienda de un vistazo sin leer. En modo oscuro los pasteles
 * del mockup encandilan: se usa el mismo color del estado, muy transparente, sobre la superficie.
 */
@Composable
private fun fondoDeEstado(estadoId: String, color: Color): Color {
    val superficie = MaterialTheme.colorScheme.surface
    if (superficie.luminance() < 0.5f) return color.copy(alpha = 0.14f).compositeOver(superficie)
    return when (estadoId) {
        EstadoCadete.LIBRE -> Color(0xFFECFDF5)
        EstadoCadete.OCUPADO -> Color(0xFFFFFBEB)
        else -> Color(0xFFFEF2F2)
    }
}

/**
 * Viajes hoy / Facturado / Conectado (spec mejoras visuales §2, cambio 3). Los números en
 * Space Grotesk para que se lean como datos. "—" mientras no cargó (o si falló: es un extra).
 */
@Composable
private fun EstadisticasDeHoy(viajes: Int?, facturado: Double?, minutosConectado: Long?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Estadistica("Viajes hoy", viajes?.toString() ?: "—", Modifier.weight(1f))
        Estadistica("Facturado", facturado?.let { formatearPesos(it) } ?: "—", Modifier.weight(1f))
        Estadistica("Conectado", minutosConectado?.let { formatearDuracion(it) } ?: "—", Modifier.weight(1f))
    }
}

@Composable
private fun Estadistica(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(valor, style = EstiloNumero.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Gray500)
        }
    }
}


/** 95 -> "1h 35m", 40 -> "40m". */
private fun formatearDuracion(minutos: Long): String =
    if (minutos < 60) "${minutos}m" else "${minutos / 60}h ${minutos % 60}m"

/**
 * Entrada de las cards (spec mejoras visuales §2, cambio 7): fade + desplazamiento corto, una
 * sola vez al aparecer. `retrasoMs` escalona varias cards seguidas.
 */
@Composable
private fun EntradaAnimada(retrasoMs: Int = 0, content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(280, delayMillis = retrasoMs)) +
            slideInVertically(tween(280, delayMillis = retrasoMs)) { alto -> alto / 6 },
    ) { content() }
}

/**
 * Anillo que se expande y se desvanece en loop alrededor del icono de estado, solo cuando el
 * cadete está LIBRE (auditoría visual 2026-09-20, del mockup del dueño). La idea es que se
 * entienda de un vistazo que está activo y puede recibir viajes, sin tener que leer el texto.
 *
 * Se escala por `graphicsLayer` y no por tamaño: escalar no recompone el layout en cada frame,
 * solo el dibujo — con el celular arriba de la moto eso importa.
 */
@Composable
private fun PulsoLibre(color: Color, modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "pulsoLibre")
    val progreso by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progresoPulso",
    )
    Box(
        modifier
            .graphicsLayer {
                val escala = 1f + 0.35f * progreso   // 1 → 1.35, como el keyframe del mockup
                scaleX = escala
                scaleY = escala
                alpha = 0.8f * (1f - progreso)       // .8 → 0
            }
            .border(2.dp, color, CircleShape),
    )
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

/**
 * Card de un viaje asignado o en curso (spec mejoras visuales §2, cambio 4): pill de estado
 * (con punto que parpadea si está en curso) y la ruta como dos puntos unidos por una línea —
 * naranja = retiro, verde = entrega — en vez de la barra lateral de color de antes.
 */
@Composable
private fun ViajeResumenCard(viaje: PedidoDto, tiempoLimiteAceptacionSeg: Int, onVerDetalle: () -> Unit) {
    val esPendiente = viaje.estado.id == EstadoPedido.PENDIENTE
    val color = if (esPendiente) Amber500 else Emerald600
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillEstado(
                    texto = if (esPendiente) "¡Viaje nuevo! Respondé pronto" else "En curso",
                    color = color,
                    parpadea = !esPendiente,
                )
                Spacer(Modifier.weight(1f))
                Text("#${viaje.numero}", style = EstiloNumero.copy(fontSize = 15.sp), color = Gray500)
            }
            if (esPendiente) {
                Spacer(Modifier.height(8.dp))
                ContadorAceptacion(viaje.asignadoEn, tiempoLimiteAceptacionSeg)
            }
            Spacer(Modifier.height(12.dp))
            RutaRetiroEntrega(viaje.origenDireccion, viaje.destinoDireccion)
            Spacer(Modifier.height(14.dp))
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

@Composable
private fun PillEstado(texto: String, color: Color, parpadea: Boolean) {
    val alphaPunto = if (parpadea) {
        val transicion = rememberInfiniteTransition(label = "puntoEnCurso")
        val a by transicion.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "alphaPunto",
        )
        a
    } else {
        1f
    }
    Row(
        Modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { alpha = alphaPunto }
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = color,
        )
    }
}
