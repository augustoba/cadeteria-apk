package com.cadeteria.cadete

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cadeteria.cadete.push.NotificationHelper
import com.cadeteria.cadete.ui.navigation.CadeteNavGraph
import com.cadeteria.cadete.ui.theme.CadeteAppTheme
import com.cadeteria.cadete.ui.theme.TemaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CadeteApp
        manejarIntent(intent)
        setContent {
            val tema by app.sessionManager.tema.collectAsState(initial = TemaApp.SISTEMA)
            CadeteAppTheme(tema = tema) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CadeteNavGraph()
                }
            }
        }
    }

    // launchMode singleTop (Manifest): con la Activity ya viva, tocar otra notificación no
    // pasa por onCreate — llega acá.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        manejarIntent(intent)
    }

    private fun manejarIntent(intent: Intent?) {
        val app = application as CadeteApp
        when (intent?.getStringExtra(NotificationHelper.EXTRA_DESTINO)) {
            NotificationHelper.DESTINO_CHAT -> app.solicitarAbrirChat()
            NotificationHelper.DESTINO_VIAJE -> {
                intent.getStringExtra(NotificationHelper.EXTRA_PEDIDO_ID)?.let(app::solicitarAbrirViaje)
            }
        }
    }
}
