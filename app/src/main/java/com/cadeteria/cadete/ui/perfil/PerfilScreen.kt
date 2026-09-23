package com.cadeteria.cadete.ui.perfil

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.data.remote.dto.CadeteActualizacionCampoDto
import com.cadeteria.cadete.data.remote.dto.CadeteConfigDto
import com.cadeteria.cadete.ui.common.AppScaffold
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.BannerInfo
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.cadeteria.cadete.ui.navigation.Routes
import com.cadeteria.cadete.ui.theme.Amber500
import com.cadeteria.cadete.ui.theme.CademCharcoal
import com.cadeteria.cadete.ui.theme.CademOrange
import com.cadeteria.cadete.ui.theme.Gray500
import com.cadeteria.cadete.ui.theme.TemaApp
import com.cadeteria.cadete.util.corregirRotacionExif
import com.cadeteria.cadete.util.crearArchivoFotoTemporal
import com.cadeteria.cadete.util.optimizarImagen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onIrDashboard: () -> Unit,
    onIrHistorial: () -> Unit,
    onCerrarSesion: () -> Unit,
    onIrAvisos: () -> Unit,
    onIrAyuda: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: PerfilViewModel = viewModel(factory = ViewModelFactory(app) { PerfilViewModel(it) })
    val cadete by vm.cadete.collectAsState()
    val miSemana by vm.miSemana.collectAsState()
    val config by vm.config.collectAsState()
    val state by vm.uiState.collectAsState()

    var passwordActual by remember { mutableStateOf("") }
    var passwordNueva by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var cbu by remember { mutableStateOf("") }
    var aliasCbu by remember { mutableStateOf("") }
    var vehiculoMarca by remember { mutableStateOf("") }
    var vehiculoModelo by remember { mutableStateOf("") }
    var vehiculoColorNuevo by remember { mutableStateOf("") }
    var vehiculoPatenteNuevo by remember { mutableStateOf("") }
    var vehiculoAnioNuevo by remember { mutableStateOf("") }

    LaunchedEffect(cadete?.id) {
        cadete?.let {
            telefono = it.telefono
            cbu = it.cbu ?: ""
            aliasCbu = it.aliasCbu ?: ""
            vehiculoMarca = it.vehiculoMarca ?: ""
            vehiculoModelo = it.vehiculoModelo ?: ""
            vehiculoColorNuevo = it.vehiculoColor ?: ""
            vehiculoPatenteNuevo = it.vehiculoPatente ?: ""
            vehiculoAnioNuevo = it.vehiculoAnio?.toString() ?: ""
        }
    }

    AppScaffold(
        title = "Configuración",
        currentRoute = Routes.PERFIL,
        cadeteNombre = cadete?.nombre,
        onIrDashboard = onIrDashboard,
        onIrHistorial = onIrHistorial,
        onIrPerfil = {},
        onCerrarSesion = onCerrarSesion,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            state.error?.let { BannerError(it, Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp)) }
            state.mensaje?.let { BannerInfo(it, Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp)) }

            cadete?.let { c ->
                PerfilHeaderCard(
                    fotoUrl = c.fotoUrl,
                    nombreCompleto = "${c.nombre} ${c.apellido}",
                    username = c.username,
                    calificacionPromedio = c.calificacionPromedio,
                    calificacionCantidad = c.calificacionCantidad,
                )
                Spacer(Modifier.height(16.dp))

                SeccionCard(titulo = "Vehículo", icono = Icons.Filled.DirectionsBike) {
                    DatoPerfil("Vehículo", c.tipoVehiculo.nombre + listOfNotNull(c.vehiculoMarca, c.vehiculoModelo).joinToString(" ", prefix = " ").ifBlank { "" })
                    DatoPerfil("Color", c.vehiculoColor ?: "—")
                    DatoPerfil("Patente", c.vehiculoPatente ?: "—")
                    DatoPerfil("DNI", c.dni)
                    c.montoMaximoTransportado?.let { DatoPerfil("Tope de dinero transportado", "$$it") }
                    c.maxViajesSimultaneos?.let { DatoPerfil("Máx. viajes simultáneos", it.toString()) }
                }
            }

            Spacer(Modifier.height(16.dp))

            SeccionCard(titulo = "Esta semana", icono = Icons.Filled.Payments) {
                miSemana?.let { semana ->
                    DatoPerfil("Facturado", "$${semana.facturado} (${semana.viajesFinalizados} viajes)")
                    DatoPerfil("¿Semana pagada?", if (semana.pagado) "✅ Sí" else "⏳ Todavía no")
                } ?: Text("Cargando…", style = MaterialTheme.typography.bodySmall, color = Gray500)
            }

            Spacer(Modifier.height(16.dp))

            cadete?.let { c -> EstadoDePagoCard(c, config) }

            Spacer(Modifier.height(16.dp))

            SeccionCard(titulo = "Teléfono", icono = Icons.Filled.Phone) {
                Text("Cambialo acá si te cambiaste de celular o de línea.", style = MaterialTheme.typography.bodySmall, color = Gray500)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.actualizarTelefono(telefono) },
                    enabled = !state.guardandoTelefono && telefono.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.guardandoTelefono) "Guardando…" else "Guardar teléfono") }
            }

            Spacer(Modifier.height(16.dp))

            SeccionCard(titulo = "Contraseña", icono = Icons.Filled.Lock) {
                OutlinedTextField(
                    value = passwordActual,
                    onValueChange = { passwordActual = it },
                    label = { Text("Contraseña actual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = passwordNueva,
                    onValueChange = { passwordNueva = it },
                    label = { Text("Contraseña nueva") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        vm.cambiarPassword(passwordActual, passwordNueva) {
                            passwordActual = ""
                            passwordNueva = ""
                        }
                    },
                    enabled = !state.guardandoPassword && passwordActual.isNotBlank() && passwordNueva.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.guardandoPassword) "Guardando…" else "Cambiar contraseña") }
            }

            Spacer(Modifier.height(16.dp))

            SeccionCard(titulo = "Datos de cobro", icono = Icons.Filled.Payments) {
                Text(
                    "Para que el cliente te pueda transferir si prefiere pagar así.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = aliasCbu,
                    onValueChange = { aliasCbu = it },
                    label = { Text("Alias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = cbu,
                    onValueChange = { cbu = it },
                    label = { Text("CBU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.actualizarCuenta(cbu, aliasCbu) },
                    enabled = !state.guardandoCuenta,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.guardandoCuenta) "Guardando…" else "Guardar datos de cobro") }
            }

            Spacer(Modifier.height(16.dp))

            cadete?.let { c ->
                val esMoto = c.tipoVehiculo.id == "MOTO"
                SeccionCard(titulo = "Actualizar mis datos", icono = Icons.Filled.DirectionsBike) {
                    Text(
                        "Los cambios quedan pendientes de revisión del admin antes de aplicarse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500,
                    )
                    Spacer(Modifier.height(10.dp))

                    CampoFotoActualizable(
                        etiqueta = "Foto de perfil",
                        fotoActualUrl = c.fotoUrl,
                        estadoPendiente = vm.ultimoEstadoDe("FOTO_PERFIL"),
                        subiendo = state.subiendoFoto == "FOTO_PERFIL",
                        onFotoElegida = { vm.proponerFoto("FOTO_PERFIL", it) },
                    )

                    if (esMoto) {
                        CampoFotoActualizable(
                            etiqueta = "Foto del vehículo",
                            fotoActualUrl = c.fotoVehiculoUrl,
                            estadoPendiente = vm.ultimoEstadoDe("FOTO_VEHICULO"),
                            subiendo = state.subiendoFoto == "FOTO_VEHICULO",
                            onFotoElegida = { vm.proponerFoto("FOTO_VEHICULO", it) },
                        )
                        CampoFotoActualizable(
                            etiqueta = "Tarjeta verde — frente",
                            fotoActualUrl = c.fotoTarjetaVerdeUrl,
                            estadoPendiente = vm.ultimoEstadoDe("FOTO_TARJETA_VERDE"),
                            subiendo = state.subiendoFoto == "FOTO_TARJETA_VERDE",
                            onFotoElegida = { vm.proponerFoto("FOTO_TARJETA_VERDE", it) },
                        )
                        CampoFotoActualizable(
                            etiqueta = "Tarjeta verde — dorso",
                            fotoActualUrl = c.fotoTarjetaVerdeDorsoUrl,
                            estadoPendiente = vm.ultimoEstadoDe("FOTO_TARJETA_VERDE_DORSO"),
                            subiendo = state.subiendoFoto == "FOTO_TARJETA_VERDE_DORSO",
                            onFotoElegida = { vm.proponerFoto("FOTO_TARJETA_VERDE_DORSO", it) },
                        )

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = vehiculoMarca, onValueChange = { vehiculoMarca = it }, label = { Text("Marca") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = vehiculoModelo, onValueChange = { vehiculoModelo = it }, label = { Text("Modelo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = vehiculoColorNuevo, onValueChange = { vehiculoColorNuevo = it }, label = { Text("Color") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = vehiculoPatenteNuevo, onValueChange = { vehiculoPatenteNuevo = it }, label = { Text("Patente") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = vehiculoAnioNuevo, onValueChange = { vehiculoAnioNuevo = it }, label = { Text("Año") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { vm.proponerDatosVehiculo(vehiculoMarca, vehiculoModelo, vehiculoColorNuevo, vehiculoPatenteNuevo, vehiculoAnioNuevo) },
                            enabled = !state.guardandoActualizacion && !vm.hayAlgoPendiente(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (state.guardandoActualizacion) "Enviando…" else "Enviar datos del vehículo para revisión") }
                    }

                    if (vm.hayAlgoPendiente()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ya tenés una actualización esperando revisión — esperá a que se resuelva antes de mandar otra.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SeccionCard(titulo = "Apariencia", icono = Icons.Filled.DarkMode) {
                val temaActual by vm.tema.collectAsState()
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val opciones = listOf(TemaApp.SISTEMA to "Sistema", TemaApp.CLARO to "Claro", TemaApp.OSCURO to "Oscuro")
                    opciones.forEachIndexed { index, (valor, etiqueta) ->
                        SegmentedButton(
                            selected = temaActual == valor,
                            onClick = { vm.cambiarTema(valor) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = opciones.size),
                        ) { Text(etiqueta) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SeccionCard(titulo = "Más", icono = Icons.Filled.HelpOutline) {
                Column {
                    OutlinedButton(onClick = onIrAvisos, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Avisos de la cadetería")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onIrAyuda, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ayuda")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onCerrarSesion,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}

@Composable
private fun PerfilHeaderCard(
    fotoUrl: String?,
    nombreCompleto: String,
    username: String,
    calificacionPromedio: Double?,
    calificacionCantidad: Long,
) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = CademCharcoal)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = optimizarImagen(fotoUrl, 240),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(nombreCompleto, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text("@$username", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))
                if (calificacionPromedio != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${"%.1f".format(calificacionPromedio)} ($calificacionCantidad viajes)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                        )
                    }
                } else {
                    Text("Sin viajes calificados todavía", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun SeccionCard(titulo: String, icono: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = CademOrange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * Modelo de cobro del cadete (ronda 7): "SEMANAL" (cuota fija, lo habilita el admin cada
 * semana) o "PORCENTAJE" (crédito precargado, se descuenta un % de cada viaje aceptado).
 */
@Composable
private fun EstadoDePagoCard(c: com.cadeteria.cadete.data.remote.dto.CadeteDto, config: CadeteConfigDto?) {
    SeccionCard(titulo = "Estado de pago", icono = Icons.Filled.Payments) {
        if (c.modalidadPago == "PORCENTAJE") {
            DatoPerfil("Saldo disponible", "$${c.creditoDisponible}")
            config?.let { DatoPerfil("Comisión por viaje", "${it.comisionPorcentaje}%") }
            Text(
                "Se te descuenta la comisión de cada viaje al aceptarlo. Si el saldo llega a $0 no vas a poder recibir más viajes hasta que te carguen más.",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500,
            )
        } else {
            val cuota = config?.pagoSemanalMonto
            val pagado = c.pagoSemanalMontoPagado ?: 0.0
            val pagoCompleto = cuota != null && pagado >= cuota
            val falta = if (cuota != null) (cuota - pagado).coerceAtLeast(0.0) else null

            DatoPerfil("¿Podés recibir viajes esta semana?", if (c.habilitadoPago) "✅ Sí, estás al día" else "⛔ No — falta pagar la cuota semanal")
            if (cuota != null) DatoPerfil("Cuota semanal", "$$cuota")
            if (c.pagoSemanalMontoPagado != null) {
                DatoPerfil("Pago", if (pagoCompleto) "✅ Total — pagaste $$pagado" else "🟡 Parcial — pagaste $$pagado")
                if (!pagoCompleto && falta != null) DatoPerfil("Te falta pagar", "$$falta")
            }
            if (c.habilitadoPago && c.pagoSemanalVenceEn != null) {
                DatoPerfil("Vencimiento del resto a pagar", formatearFechaCorta(c.pagoSemanalVenceEn))
                Text(
                    "Te habilitaron con un pago parcial — si no completás el resto antes de esa fecha, te deshabilitan solo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                )
            } else if (!c.habilitadoPago) {
                Text(
                    "Comunicate con la cadetería para pagar la cuota de esta semana y que te habiliten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                )
            }
        }
    }
}

/** "2026-09-13T02:14:37Z" -> "13/09 02:14", sin depender de java.time (minSdk 24 sin desugaring). */
private fun formatearFechaCorta(iso: String): String = runCatching {
    val fecha = iso.substringBefore("T")
    val hora = iso.substringAfter("T").take(5)
    val (_, mes, dia) = fecha.split("-")
    "$dia/$mes $hora"
}.getOrDefault(iso)

@Composable
private fun DatoPerfil(etiqueta: String, valor: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = Gray500)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CampoFotoActualizable(
    etiqueta: String,
    fotoActualUrl: String?,
    estadoPendiente: CadeteActualizacionCampoDto?,
    subiendo: Boolean,
    onFotoElegida: (File) -> Unit,
) {
    val context = LocalContext.current
    var archivoTemp by remember { mutableStateOf<File?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) {
            archivoTemp?.let {
                corregirRotacionExif(it)
                onFotoElegida(it)
            }
        }
    }

    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (fotoActualUrl != null) {
                AsyncImage(
                    model = optimizarImagen(fotoActualUrl, 160),
                    contentDescription = etiqueta,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                estadoPendiente?.let {
                    when (it.estado) {
                        "PENDIENTE" -> Text("⏳ Pendiente de revisión", style = MaterialTheme.typography.bodySmall, color = Amber500)
                        "RECHAZADO" -> Text(
                            "❌ Rechazado${it.motivoRechazo?.let { m -> ": $m" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        else -> {}
                    }
                }
            }
            OutlinedButton(
                enabled = !subiendo,
                onClick = {
                    val (archivo, uri) = crearArchivoFotoTemporal(context)
                    archivoTemp = archivo
                    launcher.launch(uri)
                },
            ) { Text(if (subiendo) "Subiendo…" else "Cambiar") }
        }
    }
}
