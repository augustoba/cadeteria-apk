package com.cadeteria.cadete.ui.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.location.LocationServiceController
import com.cadeteria.cadete.location.rememberUbicacionHabilitada
import com.cadeteria.cadete.ui.chat.ChatScreen
import com.cadeteria.cadete.ui.common.CargandoFullScreen
import com.cadeteria.cadete.ui.common.UbicacionDesactivadaScreen
import com.cadeteria.cadete.ui.historial.HistorialScreen
import com.cadeteria.cadete.ui.home.HomeScreen
import com.cadeteria.cadete.ui.login.LoginScreen
import com.cadeteria.cadete.ui.perfil.PerfilScreen
import com.cadeteria.cadete.ui.servidor.ServerConfigScreen
import com.cadeteria.cadete.ui.viaje.ViajeScreen
import kotlinx.coroutines.launch

@Composable
fun CadeteNavGraph() {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val navController: NavHostController = rememberNavController()

    val ubicacionHabilitada by rememberUbicacionHabilitada()
    if (!ubicacionHabilitada) {
        UbicacionDesactivadaScreen(onActivarUbicacion = {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        })
        return
    }

    var start by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        start = if (app.sessionManager.isLoggedIn()) Routes.HOME else Routes.LOGIN
    }
    val startDestination = start ?: run {
        CargandoFullScreen()
        return
    }

    // Token vencido o sesión tomada por otro dispositivo (spec: un solo dispositivo activo) — volver al login.
    LaunchedEffect(Unit) {
        app.sesionInvalidada.collect {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Se toca la notificación de un mensaje nuevo (MainActivity.manejarIntent) — va directo
    // al chat en vez de dejar al cadete en el Dashboard para que lo busque él.
    LaunchedEffect(Unit) {
        app.abrirChat.collect {
            if (app.sessionManager.isLoggedIn()) navController.navigate(Routes.CHAT)
        }
    }

    val scope = rememberCoroutineScope()
    var cantidadViajesQueBloquean by remember { mutableStateOf<Int?>(null) }

    /** Dashboard/Historial/Perfil son destinos hermanos (menú lateral) — se navega entre
     * ellos como pestañas, sin apilar copias ni perder el estado de scroll de cada uno. */
    fun irA(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** Logout desde cualquier pantalla (el menú lateral lo ofrece en las 3 principales) — pero
     * no si hay viajes asignados sin aceptar/rechazar o aceptados sin finalizar (mismo
     * endpoint que "Asignados y en curso" del Dashboard: PENDIENTE + EN_CURSO). */
    fun cerrarSesion() {
        scope.launch {
            val activos = app.pedidoRepository.viajesActivos().getOrDefault(emptyList())
            if (activos.isNotEmpty()) {
                cantidadViajesQueBloquean = activos.size
                return@launch
            }
            LocationServiceController.detener(context)
            app.realtimeManager.stop()
            app.authRepository.logout()
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    cantidadViajesQueBloquean?.let { cantidad ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { cantidadViajesQueBloquean = null },
            title = { androidx.compose.material3.Text("No podés salir todavía") },
            text = {
                androidx.compose.material3.Text(
                    if (cantidad == 1) {
                        "Tenés un viaje pendiente de aceptar o sin finalizar. Resolvelo antes de cerrar sesión."
                    } else {
                        "Tenés $cantidad viajes pendientes de aceptar o sin finalizar. Resolvelos antes de cerrar sesión."
                    },
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { cantidadViajesQueBloquean = null }) {
                    androidx.compose.material3.Text("Entendido")
                }
            },
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SERVIDOR) {
            ServerConfigScreen(onContinuar = { navController.popBackStack() })
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginOk = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onCambiarServidor = { navController.navigate(Routes.SERVIDOR) },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onAbrirViaje = { pedidoId -> navController.navigate(Routes.viaje(pedidoId)) },
                onAbrirChat = { navController.navigate(Routes.CHAT) },
                onIrHistorial = { irA(Routes.HISTORIAL) },
                onIrPerfil = { irA(Routes.PERFIL) },
                onCerrarSesion = ::cerrarSesion,
            )
        }
        composable(
            Routes.VIAJE,
            arguments = listOf(navArgument("pedidoId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val pedidoId = backStackEntry.arguments?.getString("pedidoId").orEmpty()
            ViajeScreen(pedidoId = pedidoId, onVolver = { navController.popBackStack() })
        }
        composable(Routes.HISTORIAL) {
            HistorialScreen(
                onAbrirViaje = { pedidoId -> navController.navigate(Routes.viaje(pedidoId)) },
                onIrDashboard = { irA(Routes.HOME) },
                onIrPerfil = { irA(Routes.PERFIL) },
                onCerrarSesion = ::cerrarSesion,
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(onVolver = { navController.popBackStack() })
        }
        composable(Routes.PERFIL) {
            PerfilScreen(
                onIrDashboard = { irA(Routes.HOME) },
                onIrHistorial = { irA(Routes.HISTORIAL) },
                onCerrarSesion = ::cerrarSesion,
            )
        }
    }
}
