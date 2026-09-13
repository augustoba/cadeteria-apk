package com.cadeteria.cadete.ui.chat

import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.MensajeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ChatUiState(
    val cargando: Boolean = true,
    val mensajes: List<MensajeDto> = emptyList(),
    val enviando: Boolean = false,
    val grabando: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(private val app: CadeteApp) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var cadeteId: String? = null
    private var recorder: MediaRecorder? = null
    private var archivoGrabacion: File? = null

    init {
        viewModelScope.launch {
            cadeteId = app.sessionManager.cadeteIdSync()
            cargar()
            escucharNuevos()
        }
    }

    fun cargar() {
        val id = cadeteId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            app.chatRepository.historial(id)
                .onSuccess { _uiState.value = _uiState.value.copy(cargando = false, mensajes = it) }
                .onFailure { _uiState.value = _uiState.value.copy(cargando = false, error = "No se pudo cargar el chat.") }
            app.chatRepository.marcarLeido(id)
            app.limpiarChatNoLeidos()
        }
    }

    fun enviar(texto: String) {
        val id = cadeteId ?: return
        if (texto.isBlank()) return
        _uiState.value = _uiState.value.copy(enviando = true, error = null)
        viewModelScope.launch {
            app.chatRepository.enviar(id, texto.trim())
                .onSuccess { nuevo ->
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        mensajes = _uiState.value.mensajes + nuevo,
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo enviar el mensaje.") }
        }
    }

    /** Nota de voz: el cadete manejando no puede tipear (spec). Graba en AAC/m4a hasta que suelta el botón. */
    fun iniciarGrabacion() {
        if (_uiState.value.grabando) return
        try {
            val archivo = File(app.cacheDir, "nota_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val rec = MediaRecorder()
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(archivo.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            archivoGrabacion = archivo
            _uiState.value = _uiState.value.copy(grabando = true, error = null)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "No se pudo empezar a grabar — revisá el permiso de micrófono.")
        }
    }

    fun cancelarGrabacion() {
        val rec = recorder ?: return
        runCatching { rec.stop() }
        rec.release()
        recorder = null
        archivoGrabacion?.delete()
        archivoGrabacion = null
        _uiState.value = _uiState.value.copy(grabando = false)
    }

    fun detenerYEnviarGrabacion() {
        val id = cadeteId
        val rec = recorder
        val archivo = archivoGrabacion
        if (id == null || rec == null || archivo == null) return
        runCatching { rec.stop() }
        rec.release()
        recorder = null
        archivoGrabacion = null
        _uiState.value = _uiState.value.copy(grabando = false, enviando = true, error = null)
        viewModelScope.launch {
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            if (config == null) {
                _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo leer la configuración del servidor — la nota de voz NO se mandó, probá de nuevo.")
                return@launch
            }
            app.cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, archivo, "audio/mp4")
                .onSuccess { url ->
                    app.chatRepository.enviarNotaDeVoz(id, url)
                        .onSuccess { nuevo ->
                            _uiState.value = _uiState.value.copy(enviando = false, mensajes = _uiState.value.mensajes + nuevo)
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(
                                enviando = false,
                                error = "La nota de voz NO se mandó — probá de nuevo (${it.message ?: "error al enviar"}).",
                            )
                        }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        error = "La nota de voz NO se mandó — no se pudo subir (${it.message ?: "error de red"}).",
                    )
                }
        }
    }

    /** Mejora 88 — adjuntar foto en el chat, ej. "esta dirección no existe". La foto ya viene elegida/tomada (`archivo`). */
    fun enviarImagen(archivo: File) {
        val id = cadeteId ?: return
        _uiState.value = _uiState.value.copy(enviando = true, error = null)
        viewModelScope.launch {
            val config = app.cadeteRepository.miConfiguracion().getOrNull()
            if (config == null) {
                _uiState.value = _uiState.value.copy(enviando = false, error = "No se pudo leer la configuración del servidor — la foto NO se mandó, probá de nuevo.")
                return@launch
            }
            app.cloudinaryUploader.subir(config.cloudinaryCloudName, config.cloudinaryUploadPreset, archivo, "image/jpeg")
                .onSuccess { url ->
                    app.chatRepository.enviarImagen(id, url)
                        .onSuccess { nuevo ->
                            _uiState.value = _uiState.value.copy(enviando = false, mensajes = _uiState.value.mensajes + nuevo)
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(
                                enviando = false,
                                error = "La foto NO se mandó — probá de nuevo (${it.message ?: "error al enviar"}).",
                            )
                        }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        error = "La foto NO se mandó — no se pudo subir (${it.message ?: "error de red"}).",
                    )
                }
        }
    }

    private fun escucharNuevos() {
        viewModelScope.launch {
            app.realtimeManager.mensajesChat.collect { nuevo ->
                if (_uiState.value.mensajes.any { it.id == nuevo.id }) return@collect
                _uiState.value = _uiState.value.copy(mensajes = _uiState.value.mensajes + nuevo)
                cadeteId?.let { app.chatRepository.marcarLeido(it) }
                app.limpiarChatNoLeidos()
            }
        }
    }
}
