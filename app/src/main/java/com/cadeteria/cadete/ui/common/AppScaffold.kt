package com.cadeteria.cadete.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import com.cadeteria.cadete.ui.navigation.Routes

/**
 * Estructura común de las 3 pantallas principales (Dashboard/Historial/Configuración).
 * Antes esto era un menú lateral (☰) — se cambió a una barra de navegación inferior
 * (auditoría UX 2026-09-15): con el celular en una mano arriba de la moto/bici, una barra
 * fija abajo queda al alcance del pulgar y cambia de pantalla en un solo toque, contra
 * dos toques (abrir el drawer, después elegir) y un ícono arriba a la izquierda, más lejos
 * del agarre natural. "Salir" pasa a ser un ícono en la barra superior, visible en las 3.
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
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title)
                        if (!cadeteNombre.isNullOrBlank()) {
                            Text(
                                "Hola, $cadeteNombre",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    actions()
                    IconButton(onClick = onCerrarSesion) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Salir",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.HOME,
                    onClick = onIrDashboard,
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Dashboard") },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.HISTORIAL,
                    onClick = onIrHistorial,
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("Historial") },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.PERFIL,
                    onClick = onIrPerfil,
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Configuración") },
                )
            }
        },
    ) { padding -> content(padding) }
}
