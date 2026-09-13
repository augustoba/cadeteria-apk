package com.cadeteria.cadete.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cadeteria.cadete.ui.navigation.Routes
import com.cadeteria.cadete.ui.theme.CademCharcoal
import kotlinx.coroutines.launch

/**
 * Estructura común de las 3 pantallas principales a las que se llega desde el menú lateral
 * (Dashboard/Historial/Perfil, ver 7 menu desplegable.jpeg en la raíz del repo) — abre con el
 * ícono ☰ en vez de una flecha de "volver", porque son destinos hermanos, no un flujo lineal.
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navegarYCerrar(accion: () -> Unit) {
        scope.launch { drawerState.close() }
        accion()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader(cadeteNombre)
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    selected = currentRoute == Routes.HOME,
                    onClick = { navegarYCerrar(onIrDashboard) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("Historial") },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    selected = currentRoute == Routes.HISTORIAL,
                    onClick = { navegarYCerrar(onIrHistorial) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("Configuración") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    selected = currentRoute == Routes.PERFIL,
                    onClick = { navegarYCerrar(onIrPerfil) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Salir") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    selected = false,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = MaterialTheme.colorScheme.error,
                        unselectedIconColor = MaterialTheme.colorScheme.error,
                    ),
                    onClick = { navegarYCerrar(onCerrarSesion) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { padding -> content(padding) }
    }
}

@Composable
private fun DrawerHeader(cadeteNombre: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CademCharcoal)
            .padding(20.dp),
    ) {
        CademWordmark(contraste = Color.White, tamano = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text("cadeteria", color = Color(0xFFBDBDBD))
        if (!cadeteNombre.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("Hola, $cadeteNombre", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}
