package com.cadeteria.cadete

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.cadeteria.cadete.data.local.OnboardingStore
import com.cadeteria.cadete.data.local.PendingActionsStore
import com.cadeteria.cadete.data.local.SessionManager
import com.cadeteria.cadete.data.remote.RetrofitProvider
import com.cadeteria.cadete.data.repository.AuthRepository
import com.cadeteria.cadete.data.repository.CadeteRepository
import com.cadeteria.cadete.data.repository.ChatRepository
import com.cadeteria.cadete.data.repository.CloudinaryUploader
import com.cadeteria.cadete.data.repository.PedidoRepository
import com.cadeteria.cadete.data.repository.PendingActionsRepository
import com.cadeteria.cadete.push.NotificationHelper
import com.cadeteria.cadete.realtime.RealtimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Service locator manual — sin Hilt/Dagger a propósito: la app es chica (una sola
 * pantalla activa a la vez, un solo usuario logueado) y así se evita depender de un
 * annotation processor más en el build de un proyecto que ya de por sí hay que armar
 * desde cero en Android Studio (ver README).
 */
class CadeteApp : Application() {

    lateinit var sessionManager: SessionManager
        private set
    lateinit var retrofitProvider: RetrofitProvider
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var cadeteRepository: CadeteRepository
        private set
    lateinit var pedidoRepository: PedidoRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var cloudinaryUploader: CloudinaryUploader
        private set
    lateinit var realtimeManager: RealtimeManager
        private set
    lateinit var pendingActionsRepository: PendingActionsRepository
        private set
    lateinit var onboardingStore: OnboardingStore
        private set

    /** Popup de bienvenida con el saldo al entrar (solo cadetes PORCENTAJE) — se prende en el login y HomeViewModel lo consume una sola vez. */
    var mostrarBienvenidaAlEntrar = false

    /** Popup de recordatorios (documentación, casco/cadena/mochila, marcar retirado/finalizado)
     * al entrar — mismo mecanismo que la bienvenida, una sola vez por login. */
    var mostrarRecordatoriosAlEntrar = false

    /** Se emite cuando el backend devuelve 401 (token vencido o sesión tomada por otro
     * dispositivo — spec: un solo dispositivo activo por cuenta) para que la navegación
     * vuelva sola al login. */
    private val _sesionInvalidada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sesionInvalidada: SharedFlow<Unit> = _sesionInvalidada.asSharedFlow()

    /** Badge del ícono de Chat en el Dashboard — mensajes del admin que llegaron y todavía
     * no se vieron en la pantalla de Chat. Vive acá (no en un ViewModel) para no perderse
     * al navegar entre Dashboard/Historial/Perfil. */
    private val _chatNoLeidos = MutableStateFlow(0)
    val chatNoLeidos: StateFlow<Int> = _chatNoLeidos.asStateFlow()
    fun sumarChatNoLeido() { _chatNoLeidos.value += 1 }
    fun limpiarChatNoLeidos() { _chatNoLeidos.value = 0 }

    /** Tocar la notificación de un mensaje nuevo tiene que abrir el Chat directo, no el
     * Dashboard — MainActivity emite acá cuando el intent trae el extra correspondiente. */
    private val _abrirChat = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val abrirChat: SharedFlow<Unit> = _abrirChat.asSharedFlow()
    fun solicitarAbrirChat() { _abrirChat.tryEmit(Unit) }

    /** Tocar la notificación de "Nuevo viaje" va directo al detalle de ese pedido — con el
     * tiempo límite para aceptar corriendo, no tiene sentido hacer pasar al cadete por el
     * Dashboard a buscar la tarjeta (auditoría UX 2026-09-15). */
    private val _abrirViaje = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val abrirViaje: SharedFlow<String> = _abrirViaje.asSharedFlow()
    fun solicitarAbrirViaje(pedidoId: String) { _abrirViaje.tryEmit(pedidoId) }

    /** Para el flush de "Finalizar" pendientes cuando vuelve la conexión — no está atado al ciclo de vida de ninguna pantalla. */
    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        retrofitProvider = RetrofitProvider(sessionManager) { _sesionInvalidada.tryEmit(Unit) }
        authRepository = AuthRepository(retrofitProvider, sessionManager)
        cadeteRepository = CadeteRepository(retrofitProvider)
        pedidoRepository = PedidoRepository(retrofitProvider)
        chatRepository = ChatRepository(retrofitProvider)
        cloudinaryUploader = CloudinaryUploader()
        realtimeManager = RealtimeManager(sessionManager)
        pendingActionsRepository = PendingActionsRepository(
            PendingActionsStore(this), pedidoRepository, cadeteRepository, cloudinaryUploader,
        )
        onboardingStore = OnboardingStore(this)

        NotificationHelper.crearCanales(this)
        configurarOsmdroid()
        registrarReintentoDeFinalizacionesPendientes()
    }

    /**
     * Modo offline básico (spec Métricas/mejoras): si "Finalizar" se encoló porque no
     * había internet, se reintenta sola apenas vuelve la conexión, y también una vez al
     * abrir la app por si ya había vuelto mientras estaba cerrada.
     */
    private fun registrarReintentoDeFinalizacionesPendientes() {
        appScope.launch { pendingActionsRepository.reintentarTodas() }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                appScope.launch { pendingActionsRepository.reintentarTodas() }
            }
        }
        runCatching {
            getSystemService(ConnectivityManager::class.java)?.registerNetworkCallback(request, callback)
        }
    }

    /** Cache en almacenamiento interno (cacheDir) para no pedir permiso de almacenamiento. */
    private fun configurarOsmdroid() {
        val prefs = getSharedPreferences("osmdroid", MODE_PRIVATE)
        Configuration.getInstance().load(this, prefs)
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = File(cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
    }
}
