package com.cadeteria.cadete.ui.recuperarpassword

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cadeteria.cadete.CadeteApp
import com.cadeteria.cadete.ui.common.BannerError
import com.cadeteria.cadete.ui.common.BannerInfo
import com.cadeteria.cadete.ui.common.CademWordmark
import com.cadeteria.cadete.ui.common.ViewModelFactory
import com.cadeteria.cadete.ui.theme.CademCharcoal
import com.cadeteria.cadete.ui.theme.Gray500

/**
 * "Olvidé mi contraseña" (spec: mandada por el dueño) — dos pasos en una sola pantalla:
 * pedir el usuario (DNI) para que llegue el código por mail, y después cambiar la
 * contraseña con ese código. Una vez que se pidió el código, el back del sistema queda
 * bloqueado a propósito ("no lo deje salir de ahí hasta que la cambie") — la única forma
 * de volver al login es terminando el cambio, o el botón explícito para corregir el
 * usuario si se equivocó.
 */
@Composable
fun RecuperarPasswordScreen(onListo: () -> Unit, onCancelar: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CadeteApp
    val vm: RecuperarPasswordViewModel = viewModel(factory = ViewModelFactory(app) { RecuperarPasswordViewModel(it) })
    val state by vm.uiState.collectAsState()

    var username by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var nuevaPassword by remember { mutableStateOf("") }
    var repetirPassword by remember { mutableStateOf("") }

    BackHandler(enabled = state.paso == PasoRecuperarPassword.PEDIR_USUARIO) { onCancelar() }
    // Paso 2: back del sistema no hace nada — hay que completar el cambio o usar "Corregir usuario".
    BackHandler(enabled = state.paso == PasoRecuperarPassword.PEDIR_CODIGO) {}

    LaunchedEffect(state.listo) {
        if (state.listo) onListo()
    }

    Scaffold(containerColor = CademCharcoal) { padding ->
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
                CademWordmark(contraste = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recuperar contraseña",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFBDBDBD),
                )
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.fillMaxWidth().padding(28.dp)) {
                    state.error?.let {
                        BannerError(it, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                    }
                    state.mensaje?.let {
                        BannerInfo(it, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                    }

                    when (state.paso) {
                        PasoRecuperarPassword.PEDIR_USUARIO -> {
                            Text(
                                "Ingresá tu usuario (DNI) — si tenés un mail cargado, te mandamos un código.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500,
                            )
                            Spacer(Modifier.height(20.dp))
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Usuario") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(20.dp))
                            BotonPrincipal(cargando = state.cargando, texto = "Mandar código") { vm.pedirCodigo(username) }
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
                                Text("Volver a ingresar")
                            }
                        }

                        PasoRecuperarPassword.PEDIR_CODIGO -> {
                            Text(
                                "Escribí el código que te llegó por mail y tu contraseña nueva.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500,
                            )
                            Spacer(Modifier.height(20.dp))
                            OutlinedTextField(
                                value = codigo,
                                onValueChange = { codigo = it },
                                label = { Text("Código (6 dígitos)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = nuevaPassword,
                                onValueChange = { nuevaPassword = it },
                                label = { Text("Contraseña nueva") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = repetirPassword,
                                onValueChange = { repetirPassword = it },
                                label = { Text("Repetir contraseña") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(20.dp))
                            BotonPrincipal(cargando = state.cargando, texto = "Cambiar contraseña") {
                                vm.confirmar(codigo, nuevaPassword, repetirPassword)
                            }
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { vm.volverAPedirUsuario() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Puse mal el usuario, corregir")
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No podés salir de esta pantalla hasta cambiar la contraseña.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonPrincipal(cargando: Boolean, texto: String, onClick: () -> Unit) {
    if (cargando) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.padding(8.dp))
        }
    } else {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) { Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    }
}
