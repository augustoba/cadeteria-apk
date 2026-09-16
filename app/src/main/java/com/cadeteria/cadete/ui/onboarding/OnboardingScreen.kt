package com.cadeteria.cadete.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cadeteria.cadete.ui.theme.CademOrange
import com.cadeteria.cadete.ui.theme.Gray500
import kotlinx.coroutines.launch

private data class PasoOnboarding(val icono: ImageVector, val titulo: String, val texto: String)

private val PASOS = listOf(
    PasoOnboarding(
        Icons.Filled.PowerSettingsNew,
        "Activate para recibir viajes",
        "Tocá el botón grande \"Activarme\" en el Dashboard cuando arranques tu turno. Mientras estés desconectado, no te va a llegar ningún viaje nuevo.",
    ),
    PasoOnboarding(
        Icons.Filled.Timer,
        "Tenés un tiempo límite para responder",
        "Cuando te llega un viaje, vas a ver una cuenta regresiva — tenés que aceptarlo o rechazarlo antes de que se acabe el tiempo, si no se le ofrece a otro cadete.",
    ),
    PasoOnboarding(
        Icons.Filled.Map,
        "Navegá con Maps o Waze",
        "Dentro de un viaje tenés botones para abrir la ruta directo en Google Maps o Waze — usá la app de navegación con la que ya estás cómodo manejando.",
    ),
    PasoOnboarding(
        Icons.Filled.Campaign,
        "Chat y avisos de la cadetería",
        "Cada viaje tiene su propio chat para coordinar con el admin. Los avisos generales (horarios, novedades) te llegan como notificación y quedan guardados en \"Avisos\", dentro de tu perfil.",
    ),
)

/**
 * Tutorial de bienvenida (mejora 2026-09-16) — se muestra una sola vez, la primera vez que
 * el cadete entra a la app (ver OnboardingStore). Antes un cadete nuevo caía directo al
 * Dashboard sin que nadie le explicara cómo funciona activarse, la cuenta regresiva, etc.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onContinuar: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PASOS.size })
    val scope = rememberCoroutineScope()
    val esUltimo = pagerState.currentPage == PASOS.lastIndex

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onContinuar) { Text("Saltar") }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pagina ->
            val paso = PASOS[pagina]
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(CademOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(paso.icono, contentDescription = null, tint = CademOrange, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    paso.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    paso.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            PASOS.indices.forEach { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) CademOrange else Color.LightGray),
                )
            }
        }

        Button(
            onClick = {
                if (esUltimo) {
                    onContinuar()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CademOrange),
        ) {
            Text(if (esUltimo) "Empezar" else "Siguiente")
        }
    }
}
