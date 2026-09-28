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
    /**
     * Control "en el lugar" (2026-09-28): hora del toque (ISO UTC), precisión del GPS y si usó
     * "Estoy en el lugar". Nullable: lo encolado por la versión anterior no los tiene (Gson deja null).
     */
    val tocadoEn: String? = null,
    val precision: Float? = null,
    val enElLugar: Boolean? = null,
)

/** Mismo caso que FinalizarPendiente pero para el botón "Marcar como retirado" (ronda 3, punto 24). */
data class RetiradoPendiente(
    val pedidoId: String,
    val fotoUrl: String?,
    val fotoPathLocal: String?,
    val lat: Double?,
    val lng: Double?,
    val tocadoEn: String? = null,
    val precision: Float? = null,
    val enElLugar: Boolean? = null,
)

/** Parada entregada sin señal (2026-09-28): antes la parada no se encolaba (y no mandaba posición). */
data class ParadaPendiente(
    val pedidoId: String,
    val paradaId: String,
    val fotoUrl: String?,
    val fotoPathLocal: String?,
    val lat: Double?,
    val lng: Double?,
    val tocadoEn: String?,
    val precision: Float?,
    val enElLugar: Boolean?,
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

    private val keyParadas = stringPreferencesKey("paradas_pendientes")
    private val tipoListaParadas = object : TypeToken<List<ParadaPendiente>>() {}.type

    suspend fun agregarParada(item: ParadaPendiente) {
        context.pendingActionsDataStore.edit { prefs ->
            val actuales = leerDeParadas(prefs[keyParadas]).filter { it.paradaId != item.paradaId }
            prefs[keyParadas] = gson.toJson(actuales + item)
        }
    }

    suspend fun listarParadas(): List<ParadaPendiente> = leerDeParadas(context.pendingActionsDataStore.data.first()[keyParadas])

    suspend fun quitarParada(paradaId: String) {
        context.pendingActionsDataStore.edit { prefs ->
            prefs[keyParadas] = gson.toJson(leerDeParadas(prefs[keyParadas]).filter { it.paradaId != paradaId })
        }
    }

    private fun leerDeParadas(json: String?): List<ParadaPendiente> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { gson.fromJson<List<ParadaPendiente>>(json, tipoListaParadas) }.getOrDefault(emptyList())
    }
}
