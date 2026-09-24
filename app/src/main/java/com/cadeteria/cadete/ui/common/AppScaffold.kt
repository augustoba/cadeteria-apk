package com.cadeteria.cadete.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Miércoles 24 de septiembre" — con SimpleDateFormat y no java.time (minSdk 24 sin desugaring). */
private fun fechaDeHoy(): String =
    SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "AR")).format(Date()).replaceFirstChar { it.uppercase() }

/**
 * Estructura común de las pantallas principales (Inicio/Historial/Chat/Configuración).
 * La navegación es una barra inferior (auditoría UX 2026-09-15): con el celular en una mano
 * arriba de la moto/bici, una barra fija abajo queda al alcance del pulgar.
 *
 * El ☰ de arriba (spec-app-mejoras-visuales §2, decisión 2) NO navega entre pantallas — para
 * eso está la barra — : es el menú de la cuenta (Ayuda y Salir). En Inicio la barra superior
 * es el saludo con la fecha en vez del título.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    currentRoute: String,
    cadeteNombre: String?,
    onIrDashboard: () -> Unit,
    onIrHistorial: () -> Unit,
    onIrPerfil: () -> Unit,
    onCerrarSesion: () -> Unit,
    onIrChat: () -> Unit = {},
    onIrAyuda: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val app = LocalContext.current.applicationContext as CadeteApp
    val chatNoLeidos by app.chatNoLeidos.collectAsState()
    var menuAbierto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menú de la cuenta")
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text("Ayuda") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
                            onClick = {
                                menuAbierto = false
                                onIrAyuda()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Salir", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                menuAbierto = false
                                onCerrarSesion()
                            },
                        )
                    }
                },
                title = {
                    if (currentRoute == Routes.HOME && !cadeteNombre.isNullOrBlank()) {
                        Column {
                            Text("Hola, $cadeteNombre 👋", fontWeight = FontWeight.Bold)
                            Text(
                                fechaDeHoy(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(title)
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.HOME,
                    onClick = onIrDashboard,
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Inicio") },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.HISTORIAL,
                    onClick = onIrHistorial,
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("Historial") },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.CHAT,
                    onClick = onIrChat,
                    icon = {
                        BadgedBox(badge = {
                            if (chatNoLeidos > 0) Badge { Text(if (chatNoLeidos > 9) "9+" else "$chatNoLeidos") }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                        }
                    },
                    label = { Text("Chat") },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.PERFIL,
                    onClick = onIrPerfil,
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Ajustes") },  // "Configuración" no entra en 4 pestañas
                )
            }
        },
    ) { padding -> content(padding) }
}
