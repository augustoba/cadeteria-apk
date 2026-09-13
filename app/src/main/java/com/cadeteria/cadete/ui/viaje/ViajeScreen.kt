package com.cadeteria.cadete.ui.viaje

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.EstadoPedido
import com.cadeteria.cadete.data.remote.dto.PedidoDto
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.BannerInfo
import com.cadeteria.cadete.ui.common.CargandoFullScreen
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViajeScreen(pedidoId: String, onVolver: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: ViajeViewModel = viewModel(
        key = pedidoId,
        factory = ViewModelFactory(app) { ViajeViewModel(it, pedidoId) },
    )
    val state by vm.uiState.collectAsState()

    LaunchedEffect(state.terminado, state.preguntarSiSigueLibre) {
        if (state.terminado && !state.preguntarSiSigueLibre) onVolver()
    }

    if (state.preguntarSiSigueLibre) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Ya no tenés viajes activos") },
            text = { Text("¿Seguís disponible para recibir viajes o te desactivás?") },
            confirmButton = {
                TextButton(onClick = { vm.resolverPreguntaLibre(seguirLibre = true) }) { Text("Seguir disponible") }
            },
            dismissButton = {
                TextButton(onClick = { vm.resolverPreguntaLibre(seguirLibre = false) }) { Text("Desactivarme") }
            },
        )
    }

    if (state.preguntarSiQuedaOcupado) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Viaje aceptado") },
            text = { Text("¿Querés seguir recibiendo pedidos nuevos o te ponés ocupado mientras hacés este viaje?") },
            confirmButton = {
                TextButton(onClick = { vm.resolverPreguntaOcupado(ponerseOcupado = false) }) { Text("Sí, seguir recibiendo") }
            },
            dismissButton = {
                TextButton(onClick = { vm.resolverPreguntaOcupado(ponerseOcupado = true) }) { Text("Ponerme ocupado") }
            },
        )
    }

    var mostrarFinalizar by remember { mutableStateOf(false) }
    var mostrarRechazar by remember { mutableStateOf(false) }
    var mostrarComentario by remember { mutableStateOf(false) }
    var mostrarNoEntregado by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu viaje") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        val viaje = state.viaje
        if (state.cargando || viaje == null) {
            CargandoFullScreen()
            return@Scaffold
        }

        if (state.finalizarEncolado) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                BannerInfo(
                    "📶 Sin conexión — guardamos la finalización y la vamos a mandar sola apenas vuelva internet, no hace falta que hagas nada más.",
                    Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            state.error?.let {
                BannerError(it, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            if (state.retiradoEncolado) {
                BannerInfo(
                    "📶 Sin conexión — guardamos que marcaste el retiro y lo vamos a mandar solo apenas vuelva internet.",
                    Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }

            Text("Pedido Nº ${viaje.numero}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            MapaViaje(viaje, state.ruta?.features?.firstOrNull()?.geometry?.coordinates)
            Spacer(Modifier.height(16.dp))

            Text("Origen", style = MaterialTheme.typography.labelLarge)
            Text(viaje.origenDireccion)
            Spacer(Modifier.height(8.dp))
            Text("Destino", style = MaterialTheme.typography.labelLarge)
            Text(viaje.destinoDireccion)
            Spacer(Modifier.height(8.dp))
            Text("Precio", style = MaterialTheme.typography.labelLarge)
            Text("$${viaje.precio}")
            if (viaje.montoDeclarado != null && viaje.montoDeclarado > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Va con dinero", style = MaterialTheme.typography.labelLarge)
                Text("$${viaje.montoDeclarado}")
            }
            if (!viaje.detalle.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Detalle", style = MaterialTheme.typography.labelLarge)
                Text(viaje.detalle)
            }

            Spacer(Modifier.height(12.dp))

            val yaRetirado = !viaje.retiradoEn.isNullOrBlank() || state.retiradoEncolado
            val destinoNavegacion = if (yaRetirado) viaje.destinoLat to viaje.destinoLng else viaje.origenLat to viaje.origenLng
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { llamarACliente(context, viaje.clienteTelefono) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("📞 Llamar al cliente") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { abrirEnMaps(context, destinoNavegacion.first, destinoNavegacion.second) },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("🗺 Maps") }
                    OutlinedButton(
                        onClick = { abrirEnWaze(context, destinoNavegacion.first, destinoNavegacion.second) },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("🧭 Waze") }
                }
            }

            Spacer(Modifier.height(20.dp))

            when (viaje.estado.id) {
                EstadoPedido.PENDIENTE -> AccionesPendiente(
                    enviando = state.enviando,
                    asignadoEn = viaje.asignadoEn,
                    tiempoLimiteSeg = state.tiempoLimiteAceptacionSeg,
                    onAceptar = vm::aceptar,
                    onRechazar = { mostrarRechazar = true },
                )
                EstadoPedido.EN_CURSO -> AccionesEnCurso(
                    viaje = viaje,
                    yaRetirado = yaRetirado,
                    enviando = state.enviando,
                    onMarcarRetirado = { foto -> vm.marcarRetirado(foto) },
                    onParadaEntregadaClick = { paradaId -> vm.marcarParadaEntregada(paradaId) },
                    onFinalizarClick = { mostrarFinalizar = true },
                    onNoEntregadoClick = { mostrarNoEntregado = true },
                    onComentarioClick = { mostrarComentario = true },
                )
                else -> BannerInfo("Este viaje ya está ${viaje.estado.nombre.lowercase()}.")
            }
        }
    }

    if (mostrarFinalizar) {
        FinalizarDialog(
            enviando = state.enviando,
            firmaObligatoria = state.firmaReceptorObligatoria,
            onDismiss = { mostrarFinalizar = false },
            onConfirmar = { receptor, foto, firma ->
                vm.finalizar(receptor, foto, firma)
                mostrarFinalizar = false
            },
        )
    }

    if (mostrarRechazar) {
        RechazarDialog(
            enviando = state.enviando,
            onDismiss = { mostrarRechazar = false },
            onConfirmar = { motivo ->
                vm.rechazar(motivo)
                mostrarRechazar = false
            },
        )
    }

    if (mostrarComentario) {
        ComentarioDialog(
            enviando = state.enviandoComentario,
            onDismiss = { mostrarComentario = false },
            onConfirmar = { texto ->
                vm.agregarComentario(texto)
                mostrarComentario = false
            },
        )
    }

    if (mostrarNoEntregado) {
        NoEntregadoDialog(
            enviando = state.enviando,
            onDismiss = { mostrarNoEntregado = false },
            onConfirmar = { motivo ->
                vm.marcarNoEntregado(motivo)
                mostrarNoEntregado = false
            },
        )
    }
}

@Composable
private fun RechazarDialog(enviando: Boolean, onDismiss: () -> Unit, onConfirmar: (String?) -> Unit) {
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        title = { Text("Rechazar viaje") },
        text = {
            Column {
                Text("¿Por qué lo rechazás? Es opcional, pero nos ayuda a mejorar las asignaciones.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(enabled = !enviando, onClick = { onConfirmar(motivo.ifBlank { null }) }) {
                Text(if (enviando) "Enviando…" else "Rechazar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !enviando) { Text("Volver") }
        },
    )
}

@Composable
private fun NoEntregadoDialog(enviando: Boolean, onDismiss: () -> Unit, onConfirmar: (String?) -> Unit) {
    var motivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        title = { Text("No se pudo entregar") },
        text = {
            Column {
                Text(
                    "El pedido no se anula — queda para que el admin lo reintente. Contanos qué pasó " +
                        "(ej. \"el cliente no atendió\"), es opcional.",
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(enabled = !enviando, onClick = { onConfirmar(motivo.ifBlank { null }) }) {
                Text(if (enviando) "Guardando…" else "Confirmar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !enviando) { Text("Volver") }
        },
    )
}

/** "2026-09-13T02:14:37Z" -> millis desde epoch (UTC), sin depender de java.time (minSdk 24 sin desugaring). */
private fun parsearInstanteUtc(iso: String): Long? = runCatching {
    val formato = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
    formato.timeZone = java.util.TimeZone.getTimeZone("UTC")
    formato.parse(iso.take(19))?.time
}.getOrNull()

@Composable
private fun AccionesPendiente(
    enviando: Boolean,
    asignadoEn: String?,
    tiempoLimiteSeg: Int,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
) {
    if (enviando) {
        CircularProgressIndicator()
        return
    }

    val inicioMs = remember(asignadoEn) { asignadoEn?.let(::parsearInstanteUtc) }
    var segundosRestantes by remember(asignadoEn) { mutableStateOf<Int?>(null) }
    LaunchedEffect(inicioMs, tiempoLimiteSeg) {
        if (inicioMs == null) return@LaunchedEffect
        while (true) {
            val transcurridoSeg = (System.currentTimeMillis() - inicioMs) / 1000
            val restante = (tiempoLimiteSeg - transcurridoSeg).toInt()
            segundosRestantes = restante
            if (restante <= 0) break
            kotlinx.coroutines.delay(1000)
        }
    }

    Column {
        Text(
            "Tenés un viaje nuevo — si no lo tomás a tiempo se le ofrece a otro cadete.",
            style = MaterialTheme.typography.bodyMedium,
        )
        segundosRestantes?.let { restante ->
            Spacer(Modifier.height(6.dp))
            val urgente = restante <= 15
            Text(
                if (restante > 0) "⏱ Te quedan $restante segundos para responder" else "⏱ Se agotó el tiempo — puede que ya se le ofrezca a otro cadete",
                style = MaterialTheme.typography.titleMedium,
                color = if (urgente) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onAceptar, modifier = Modifier.weight(1f)) { Text("Aceptar") }
            OutlinedButton(onClick = onRechazar, modifier = Modifier.weight(1f)) { Text("Rechazar") }
        }
    }
}

@Composable
private fun ComentarioDialog(enviando: Boolean, onDismiss: () -> Unit, onConfirmar: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        title = { Text("Agregar comentario") },
        text = {
            Column {
                Text("Cualquier cosa que quieras dejar anotada sobre este pedido (ej. \"entregado en portería a Fulano\").")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Comentario") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(enabled = !enviando && texto.isNotBlank(), onClick = { onConfirmar(texto.trim()) }) {
                Text(if (enviando) "Guardando…" else "Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !enviando) { Text("Cancelar") }
        },
    )
}

@Composable
private fun AccionesEnCurso(
    viaje: PedidoDto,
    yaRetirado: Boolean,
    enviando: Boolean,
    onMarcarRetirado: (File?) -> Unit,
    onParadaEntregadaClick: (String) -> Unit,
    onFinalizarClick: () -> Unit,
    onNoEntregadoClick: () -> Unit,
    onComentarioClick: () -> Unit,
) {
    val context = LocalContext.current
    var fotoRetiro by remember { mutableStateOf<Bitmap?>(null) }
    val tomarFotoRetiro = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        fotoRetiro = bitmap
    }

    if (enviando) {
        CircularProgressIndicator()
        return
    }

    val paradasPendientes = viaje.paradas.filter { it.entregadoEn == null }

    Column {
        if (!yaRetirado) {
            Text(
                "Cuando pases por lo del cliente a buscar el pedido, marcalo acá (la foto es opcional).",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            fotoRetiro?.let {
                Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.height(120.dp))
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = { tomarFotoRetiro.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (fotoRetiro == null) "Sacar foto del retiro (opcional)" else "Sacar otra foto")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onMarcarRetirado(fotoRetiro?.let { guardarBitmapTemporal(context, it) }) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("📦 Marcar como retirado") }
            Spacer(Modifier.height(16.dp))
        } else {
            BannerInfo("Ya marcaste que retiraste el pedido.")
            Spacer(Modifier.height(16.dp))
        }

        if (viaje.paradas.isNotEmpty()) {
            Text("Paradas de esta vuelta", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            for (parada in viaje.paradas.sortedBy { it.orden }) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("${parada.orden}. ${parada.direccion}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (parada.entregadoEn != null) {
                        Text("✅", style = MaterialTheme.typography.bodyMedium)
                    } else if (yaRetirado) {
                        OutlinedButton(onClick = { onParadaEntregadaClick(parada.id) }) { Text("Entregada") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(onClick = onFinalizarClick, enabled = paradasPendientes.isEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Finalizar viaje")
        }
        if (paradasPendientes.isNotEmpty()) {
            Text(
                "Marcá todas las paradas como entregadas antes de finalizar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (yaRetirado) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNoEntregadoClick, modifier = Modifier.fillMaxWidth()) {
                Text("⚠️ No se pudo entregar")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onComentarioClick, modifier = Modifier.fillMaxWidth()) {
            Text("💬 Agregar comentario")
        }
    }
}

@Composable
private fun FinalizarDialog(
    enviando: Boolean,
    firmaObligatoria: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (String?, File?, File?) -> Unit,
) {
    val context = LocalContext.current
    var receptor by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        foto = bitmap
    }
    val trazosFirma = remember { mutableStateListOf<List<Offset>>() }
    var tamanoFirma by remember { mutableStateOf(IntSize.Zero) }
    val hayFirma = trazosFirma.isNotEmpty()

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        title = { Text("Finalizar viaje") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Para finalizar hace falta el nombre y apellido de quien recibió el pedido, y una foto de la entrega (o del frente del domicilio).")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = receptor,
                    onValueChange = { receptor = it },
                    label = { Text("Nombre y apellido de quien recibió") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                foto?.let {
                    Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.height(120.dp))
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(onClick = { tomarFoto.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (foto == null) "Sacar foto de la entrega / domicilio" else "Sacar otra foto")
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    if (firmaObligatoria) "Firma de quien recibió (obligatoria)" else "Firma de quien recibió (opcional)",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                FirmaPad(
                    trazos = trazosFirma,
                    onSizeChanged = { tamanoFirma = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { trazosFirma.clear() }, enabled = hayFirma) { Text("Borrar firma") }
            }
        },
        confirmButton = {
            Button(
                enabled = !enviando && receptor.isNotBlank() && foto != null && (!firmaObligatoria || hayFirma),
                onClick = {
                    val archivoFoto = foto?.let { guardarBitmapTemporal(context, it) }
                    val archivoFirma = if (hayFirma) {
                        trazosABitmap(trazosFirma, tamanoFirma.width, tamanoFirma.height)
                            ?.let { guardarBitmapTemporal(context, it) }
                    } else {
                        null
                    }
                    onConfirmar(receptor.ifBlank { null }, archivoFoto, archivoFirma)
                },
            ) { Text(if (enviando) "Enviando…" else "Confirmar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !enviando) { Text("Cancelar") }
        },
    )
}

/** Lienzo simple para que el receptor firme con el dedo (spec: firma digital al finalizar). */
@Composable
private fun FirmaPad(trazos: SnapshotStateList<List<Offset>>, onSizeChanged: (IntSize) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .onSizeChanged(onSizeChanged)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> trazos.add(listOf(offset)) },
                    onDrag = { change, _ ->
                        change.consume()
                        val idx = trazos.lastIndex
                        if (idx >= 0) trazos[idx] = trazos[idx] + change.position
                    },
                )
            },
    ) {
        for (trazo in trazos) {
            if (trazo.size < 2) continue
            val path = Path().apply {
                moveTo(trazo.first().x, trazo.first().y)
                for (i in 1 until trazo.size) lineTo(trazo[i].x, trazo[i].y)
            }
            drawPath(path, color = Color.Black, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Convierte los trazos dibujados a un bitmap blanco con líneas negras, del mismo tamaño que el lienzo. */
private fun trazosABitmap(trazos: List<List<Offset>>, ancho: Int, alto: Int): Bitmap? {
    if (ancho <= 0 || alto <= 0) return null
    val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    for (trazo in trazos) {
        if (trazo.size < 2) continue
        val path = android.graphics.Path()
        path.moveTo(trazo.first().x, trazo.first().y)
        for (i in 1 until trazo.size) path.lineTo(trazo[i].x, trazo[i].y)
        canvas.drawPath(path, paint)
    }
    return bitmap
}

@Composable
private fun MapaViaje(viaje: PedidoDto, ruta: List<List<Double>>?) {
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                // Sin esto, el mapa queda en su propia capa de hardware y se dibuja arriba
                // de todo lo demás en Compose (el texto de Origen/Destino terminaba tapado),
                // sin importar el orden real dentro de la Column.
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            }
        },
        update = { map ->
            map.overlays.clear()
            val destino = GeoPoint(viaje.destinoLat, viaje.destinoLng)
            map.overlays.add(Marker(map).apply {
                position = destino
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Destino"
            })

            var centro = destino
            if (!ruta.isNullOrEmpty()) {
                val puntos = ruta.map { GeoPoint(it[1], it[0]) } // ORS = [lng, lat]
                map.overlays.add(Polyline(map).apply { setPoints(puntos) })
                centro = puntos.first()
            } else try {
                LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            map.overlays.add(Marker(map).apply {
                                position = GeoPoint(loc.latitude, loc.longitude)
                                title = "Vos"
                            })
                            map.invalidate()
                        }
                    }
            } catch (e: SecurityException) {
                // Sin permiso de ubicación todavía — el mapa igual muestra el destino.
            }
            map.controller.setCenter(centro)
            map.invalidate()
        },
    )
}

private fun llamarACliente(context: Context, telefono: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")))
}

/** Google Maps con navegación directa; si la app no está instalada, cae al navegador. */
private fun abrirEnMaps(context: Context, lat: Double, lng: Double) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=d"))
        intent.setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")))
    }
}

/** Waze con navegación directa; si la app no está instalada, cae al navegador. */
private fun abrirEnWaze(context: Context, lat: Double, lng: Double) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://ul?ll=$lat,$lng&navigate=yes"))
        intent.setPackage("com.waze")
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes")))
    }
}

private fun guardarBitmapTemporal(context: android.content.Context, bitmap: Bitmap): File {
    val archivo = File(context.cacheDir, "foto_${System.currentTimeMillis()}.jpg")
    FileOutputStream(archivo).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
    return archivo
}
