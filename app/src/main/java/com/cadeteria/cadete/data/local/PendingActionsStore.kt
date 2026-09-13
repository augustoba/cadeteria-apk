package com.cadeteria.cadete.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

private val Context.pendingActionsDataStore by preferencesDataStore(name = "cadete_pendientes")

/**
 * Un "Finalizar" que no se pudo mandar al servidor por falta de conexión — a lo sumo uno
 * de `fotoUrl`/`fotoPathLocal` y uno de `firmaUrl`/`firmaPathLocal` (ya subida a Cloudinary,
 * o archivo en cache todavía sin subir) está cargado para cada uno.
 */
data class FinalizarPendiente(
    val pedidoId: String,
    val receptorNombre: String?,
    val fotoUrl: String?,
    val fotoPathLocal: String?,
    val firmaUrl: String?,
    val firmaPathLocal: String?,
    val lat: Double?,
    val lng: Double?,
)

/** Mismo caso que FinalizarPendiente pero para el botón "Marcar como retirado" (ronda 3, punto 24). */
data class RetiradoPendiente(
    val pedidoId: String,
    val fotoUrl: String?,
    val fotoPathLocal: String?,
    val lat: Double?,
    val lng: Double?,
)

/**
 * Cola local de "Finalizar"/"Retirado" pendientes de reenviar (spec: modo offline básico —
 * "si se corta internet justo al tocar el botón, encolar y reintentar solo cuando vuelva la
 * conexión"). PendingActionsRepository es quien la vacía.
 */
class PendingActionsStore(private val context: Context) {

    private val key = stringPreferencesKey("finalizaciones_pendientes")
    private val keyRetiros = stringPreferencesKey("retiros_pendientes")
    private val gson = Gson()
    private val tipoLista = object : TypeToken<List<FinalizarPendiente>>() {}.type
    private val tipoListaRetiros = object : TypeToken<List<RetiradoPendiente>>() {}.type

    suspend fun agregar(item: FinalizarPendiente) {
        context.pendingActionsDataStore.edit { prefs ->
            val actuales = leerDe(prefs[key]).filter { it.pedidoId != item.pedidoId }
            prefs[key] = gson.toJson(actuales + item)
        }
    }

    suspend fun listar(): List<FinalizarPendiente> = leerDe(context.pendingActionsDataStore.data.first()[key])

    suspend fun quitar(pedidoId: String) {
        context.pendingActionsDataStore.edit { prefs ->
            prefs[key] = gson.toJson(leerDe(prefs[key]).filter { it.pedidoId != pedidoId })
        }
    }

    suspend fun agregarRetiro(item: RetiradoPendiente) {
        context.pendingActionsDataStore.edit { prefs ->
            val actuales = leerDeRetiros(prefs[keyRetiros]).filter { it.pedidoId != item.pedidoId }
            prefs[keyRetiros] = gson.toJson(actuales + item)
        }
    }

    suspend fun listarRetiros(): List<RetiradoPendiente> = leerDeRetiros(context.pendingActionsDataStore.data.first()[keyRetiros])

    suspend fun quitarRetiro(pedidoId: String) {
        context.pendingActionsDataStore.edit { prefs ->
            prefs[keyRetiros] = gson.toJson(leerDeRetiros(prefs[keyRetiros]).filter { it.pedidoId != pedidoId })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun leerDe(json: String?): List<FinalizarPendiente> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { gson.fromJson<List<FinalizarPendiente>>(json, tipoLista) }.getOrDefault(emptyList())
    }

    @Suppress("UNCHECKED_CAST")
    private fun leerDeRetiros(json: String?): List<RetiradoPendiente> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { gson.fromJson<List<RetiradoPendiente>>(json, tipoListaRetiros) }.getOrDefault(emptyList())
    }
}
