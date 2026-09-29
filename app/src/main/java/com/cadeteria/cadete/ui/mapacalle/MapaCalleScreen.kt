package com.cadeteria.cadete.ui.mapacalle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.AvisoCalleDto
import com.cadeteria.cadete.data.remote.dto.TipoAvisoCalle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.cadeteria.cadete.ui.common.markerAvisoCalle
import com.cadeteria.cadete.ui.common.pinDrawable
import com.cadeteria.cadete.ui.theme.MapsBlue
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

/** Centro de San Miguel de Tucumán (la zona demo): si no hay ubicación, el mapa arranca acá. */
private val CENTRO_POR_DEFECTO = GeoPoint(-26.8241, -65.2226)

/** Cada cuánto se vuelve a pedir la lista mientras la pantalla está abierta. */
private const val REFRESCO_MS = 60_000L

/**
 * "Mapa de la calle" (2026-09-29, pedido del usuario): desde el ☰, los avisos de la calle activos de
 * toda la ciudad (controles, calles cortadas, accidentes, piquetes) con un ícono por tipo, quedan ahí
 * hasta que vencen o los bajan. Es para mirar parado; manejando sigue la notificación de siempre.
 * Se pide al servidor al abrir y cada minuto, y se suman los que llegan en vivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaCalleScreen(onVolver: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val colorVos = MapsBlue.toArgb()

    var delServidor by remember { mutableStateOf<List<AvisoCalleDto>>(emptyList()) }
    var sinConexion by remember { mutableStateOf(false) }
    var recargar by remember { mutableIntStateOf(0) }
    var reloj by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var vos by remember { mutableStateOf<GeoPoint?>(null) }
    var centrado by remember { mutableStateOf(false) }
    val enVivo by app.avisosCalleStore.avisos.collectAsState()

    DisposableEffect(Unit) {
        // Como el mini mapa del viaje: la última conocida y, si no hay (recién prendido, emulador), una nueva.
        try {
            val cliente = LocationServices.getFusedLocationProviderClient(context)
            cliente.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    vos = GeoPoint(loc.latitude, loc.longitude)
                } else {
                    cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnSuccessListener { actual -> if (actual != null) vos = GeoPoint(actual.latitude, actual.longitude) }
                }
            }
        } catch (e: SecurityException) {
            // Sin permiso: el mapa igual muestra los avisos, centrado en la ciudad.
        }
        onDispose { }
    }
    LaunchedEffect(recargar) {
        while (true) {
            runCatching { app.retrofitProvider.apiService().avisosCalleTodos() }
                .onSuccess {
                    delServidor = it
                    sinConexion = false
                }
                .onFailure { sinConexion = true }
            reloj = System.currentTimeMillis()
            delay(REFRESCO_MS)
        }
    }
    val avisos = MapaCalleAvisos.combinar(delServidor, enVivo, reloj)

    var mapa by remember { mutableStateOf<MapView?>(null) }
    DisposableEffect(Unit) { onDispose { mapa?.onDetach() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de la calle") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = { recargar++ }) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.0)
                        controller.setCenter(CENTRO_POR_DEFECTO)
                        mapa = this
                    }
                },
                update = { map ->
                    InfoWindow.closeAllInfoWindowsOn(map)
                    map.overlays.clear()
                    avisos.forEach { map.overlays.add(markerAvisoCalle(map, context, it)) }
                    vos?.let { punto ->
                        map.overlays.add(Marker(map).apply {
                            position = punto
                            icon = pinDrawable(context, colorVos)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            title = "Vos"
                        })
                        // Una sola vez: después el cadete mueve el mapa y no se lo saca de donde mira.
                        if (!centrado) {
                            centrado = true
                            map.controller.setZoom(15.0)
                            map.controller.setCenter(punto)
                        }
                    }
                    map.invalidate()
                },
            )

            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(MapaCalleAvisos.resumen(avisos.size), fontWeight = FontWeight.Bold)
                    if (avisos.isNotEmpty()) {
                        Text("Tocá un ícono para ver qué pasa y dónde.", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        TipoAvisoCalle.OPCIONES.joinToString("   ") { (_, emoji, texto) -> "$emoji $texto" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (sinConexion) {
                        Text(
                            "Sin conexión: se muestran los que ya tenías.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
