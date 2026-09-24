package com.cadeteria.cadete.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cadeteria.cadete.ui.theme.Emerald600
import com.cadeteria.cadete.ui.theme.Gray500

/** Retiro (punto naranja) → entrega (punto verde), unidos por una línea vertical. */
@Composable
fun RutaRetiroEntrega(origen: String, destino: String) {
    val naranja = MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            Modifier.width(14.dp).fillMaxHeight().padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(10.dp).background(naranja, CircleShape))
            Box(
                Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            )
            Box(Modifier.size(10.dp).background(Emerald600, CircleShape))
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text("Retiro", style = MaterialTheme.typography.labelSmall, color = Gray500)
                Text(origen, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
            Column {
                Text("Entrega", style = MaterialTheme.typography.labelSmall, color = Gray500)
                Text(destino, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
        }
    }
}
