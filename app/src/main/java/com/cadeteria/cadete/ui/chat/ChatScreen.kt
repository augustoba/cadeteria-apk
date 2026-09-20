package com.cadeteria.cadete.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
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
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.grabando) {
                    Text(
                        "🔴 Grabando…",
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribí un mensaje…") },
                        enabled = !state.enviando,
                    )
                }
                if (texto.isBlank()) {
                    if (!state.grabando) {
                        IconButton(onClick = { tomarFoto.launch(null) }, enabled = !state.enviando) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = "Adjuntar foto")
                        }
                    }
                    IconButton(
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
                    ) {
                        Icon(
                            if (state.grabando) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (state.grabando) "Detener y enviar nota de voz" else "Grabar nota de voz",
                            tint = if (state.grabando) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                } else {
                    IconButton(
                        onClick = { vm.enviar(texto); texto = "" },
                        enabled = texto.isNotBlank() && !state.enviando,
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        },
    ) { padding ->
        if (state.cargando) {
            CargandoFullScreen()
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
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

@Composable
private fun BurbujaMensaje(mensaje: MensajeDto) {
    val esMio = mensaje.autor == AutorMensaje.CADETE
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (esMio) MaterialTheme.colorScheme.primary else Color(0xFFE5E7EB),
                    RoundedCornerShape(12.dp),
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
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
        }) {
            Icon(
                if (reproduciendo) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (reproduciendo) "Pausar nota de voz" else "Reproducir nota de voz",
                tint = color,
            )
        }
        Text("🎤 Nota de voz", color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
