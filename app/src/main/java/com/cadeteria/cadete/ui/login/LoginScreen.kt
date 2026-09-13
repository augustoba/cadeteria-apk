package com.cadeteria.cadete.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.CademWordmark
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.cadeteria.cadete.ui.theme.CademCharcoal
import com.cadeteria.cadete.ui.theme.Gray500

@Composable
fun LoginScreen(onLoginOk: () -> Unit, onCambiarServidor: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: LoginViewModel = viewModel(factory = ViewModelFactory(app) { LoginViewModel(it) })
    val state by vm.uiState.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }

    Scaffold(containerColor = CademCharcoal) { padding ->
        // imePadding() + scroll: con el teclado abierto no hay pantalla completa para el
        // layout fijo de antes (logo arriba, tarjeta abajo con weight()) — así el
        // contenido se corre para arriba y se puede scrollear hasta el campo que haga falta.
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
            ) {
                CademWordmark(contraste = Color.White, tamano = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "cadetería",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFBDBDBD),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ingresá con tu usuario de cadete para empezar a recibir viajes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9E9E9E),
                )
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                ) {
                    Text("Bienvenido", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Ingresá tus datos para continuar", style = MaterialTheme.typography.bodyMedium, color = Gray500)
                    Spacer(Modifier.height(24.dp))

                    state.error?.let {
                        BannerError(it, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                Icon(
                                    if (mostrarPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (mostrarPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                )
                            }
                        },
                        visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))

                    if (state.cargando) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.padding(8.dp))
                        }
                    } else {
                        Button(
                            onClick = { vm.login(username, password, onLoginOk) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) { Text("Ingresar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    }

                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onCambiarServidor, modifier = Modifier.fillMaxWidth()) {
                        Text("Cambiar servidor")
                    }
                }
            }
        }
    }
}
