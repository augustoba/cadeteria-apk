package com.cadeteria.cadete.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.AutorMensaje
import com.cadeteria.cadete.data.remote.dto.MensajeDto
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.CargandoFullScreen
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.cadeteria.cadete.ui.theme.Gray100
import com.cadeteria.cadete.ui.theme.Gray500
import com.cadeteria.cadete.ui.theme.Red50
import com.cadeteria.cadete.util.optimizarImagen
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onVolver: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: ChatViewModel = viewModel(factory = ViewModelFactory(app) { ChatViewModel(it) })
    val state by vm.uiState.collectAsState()
    var texto by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val pedirPermisoMicrofono = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) vm.iniciarGrabacion()
    }

    /** Mejora 88 — adjuntar foto en el chat, ej. "esta dirección no existe". */
    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val archivo = File(context.cacheDir, "chat_foto_${System.currentTimeMillis()}.jpg")
            FileOutputStream(archivo).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
            vm.enviarImagen(archivo)
        }
    }

    LaunchedEffect(state.mensajes.size) {
        if (state.mensajes.isNotEmpty()) listState.animateScrollToItem(state.mensajes.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat con el admin") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.grabando) {
                    val alphaGrabando by rememberInfiniteTransition(label = "grabando").animateFloat(
                        initialValue = 1f,
                        targetValue = 0.25f,
                        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                        label = "grabando-alpha",
                    )
                    Row(
                        Modifier.weight(1f).padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .graphicsLayer { alpha = alphaGrabando }
                                .background(MaterialTheme.colorScheme.error, RoundedCornerShape(50)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Grabando nota de voz…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribí un mensaje…") },
                        enabled = !state.enviando,
                        shape = RoundedCornerShape(50),
                    )
                }
                if (texto.isBlank()) {
                    if (!state.grabando) {
                        BotonCircular(onClick = { tomarFoto.launch(null) }, enabled = !state.enviando, fondo = Gray100) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = "Adjuntar foto", tint = Gray500)
                        }
                    }
                    BotonCircular(
                        onClick = {
                            if (state.grabando) {
                                vm.detenerYEnviarGrabacion()
                            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                vm.iniciarGrabacion()
                            } else {
                                pedirPermisoMicrofono.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = !state.enviando,
                        fondo = if (state.grabando) Red50 else Gray100,
                    ) {
                        Icon(
                            if (state.grabando) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (state.grabando) "Detener y enviar nota de voz" else "Grabar nota de voz",
                            tint = if (state.grabando) MaterialTheme.colorScheme.error else Gray500,
                        )
                    }
                } else {
                    BotonCircular(
                        onClick = { vm.enviar(texto); texto = "" },
                        enabled = texto.isNotBlank() && !state.enviando,
                        fondo = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                    }
                }
            }
        },
    ) { padding ->
        if (state.cargando) {
            CargandoFullScreen()
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .background(Gray100)
                .padding(padding),
        ) {
            state.error?.let {
                BannerError(it, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.mensajes, key = { it.id }) { mensaje -> BurbujaMensaje(mensaje) }
            }
        }
    }
}

/** Botón de ícono dentro de un círculo de color — mic/cámara/enviar de la barra de abajo. */
@Composable
private fun BotonCircular(
    onClick: () -> Unit,
    enabled: Boolean,
    fondo: Color,
    contenido: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .background(fondo, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, enabled = enabled) { contenido() }
    }
}

@Composable
private fun BurbujaMensaje(mensaje: MensajeDto) {
    val esMio = mensaje.autor == AutorMensaje.CADETE
    // Esquina "pico" de 4dp del lado del que habla, en vez de las 4 esquinas parejas de antes
    // — es la forma clásica de burbuja de chat, hace más fácil distinguir de un vistazo quién
    // mandó cada mensaje en una conversación larga.
    val forma = if (esMio) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (esMio) MaterialTheme.colorScheme.primary else Color.White,
                    forma,
                )
                .padding(10.dp),
        ) {
            Column {
                val imagenUrl = mensaje.imagenUrl
                val audioUrl = mensaje.audioUrl
                if (imagenUrl != null) {
                    AsyncImage(
                        model = optimizarImagen(imagenUrl, 1200),
                        contentDescription = "Foto adjunta",
                        modifier = Modifier.widthIn(max = 240.dp),
                    )
                } else if (audioUrl != null) {
                    ReproductorNotaDeVoz(audioUrl, esMio)
                } else {
                    Text(
                        mensaje.texto.orEmpty(),
                        color = if (esMio) Color.White else Color.Black,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReproductorNotaDeVoz(url: String, esMio: Boolean) {
    var reproduciendo by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(url) {
        onDispose {
            player?.release()
            player = null
        }
    }

    val color = if (esMio) Color.White else Color.Black
    // Barras de altura fija a modo de "forma de onda" — es decoración, no la amplitud real
    // del audio (igual que el resto de las apps de chat lo usan como señal visual de "esto es
    // un audio", no como un dato).
    val alturasBarras = remember { listOf(6, 14, 9, 17, 8, 12, 6) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(min = 140.dp)) {
        Box(
            Modifier
                .size(30.dp)
                .background(color.copy(alpha = 0.18f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = {
                    val actual = player
                    if (reproduciendo && actual != null) {
                        actual.pause()
                        reproduciendo = false
                    } else if (actual != null) {
                        actual.start()
                        reproduciendo = true
                    } else {
                        val nuevo = MediaPlayer()
                        runCatching {
                            nuevo.setDataSource(url)
                            nuevo.setOnCompletionListener { reproduciendo = false }
                            nuevo.setOnPreparedListener { it.start() }
                            nuevo.prepareAsync()
                        }.onFailure { nuevo.release() }
                        player = nuevo
                        reproduciendo = true
                    }
                },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    if (reproduciendo) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (reproduciendo) "Pausar nota de voz" else "Reproducir nota de voz",
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            alturasBarras.forEach { alturaDp ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height(alturaDp.dp)
                        .background(color.copy(alpha = 0.7f), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
