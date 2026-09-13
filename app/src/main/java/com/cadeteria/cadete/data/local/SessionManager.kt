package com.cadeteria.cadete.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cadeteria.cadete.BuildConfig
import com.cadeteria.cadete.ui.theme.TemaApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cadete_session")

/**
 * Sesión persistida del cadete: URL del backend (editable desde la app — no hay Play
 * Store ni distribución centralizada, la APK se instala a mano, así que el operador
 * tiene que poder apuntarla al servidor real sin recompilar), token JWT y datos básicos.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val TOKEN = stringPreferencesKey("token")
        val USERNAME = stringPreferencesKey("username")
        val CADETE_ID = stringPreferencesKey("cadete_id")
        val CADETE_NOMBRE = stringPreferencesKey("cadete_nombre")
        val TEMA = stringPreferencesKey("tema")
        val AVISO_FLOTANTE_PENDIENTE = stringPreferencesKey("aviso_flotante_pendiente")
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { it[Keys.BASE_URL] ?: BuildConfig.DEFAULT_BASE_URL }
    val token: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val cadeteId: Flow<String?> = context.dataStore.data.map { it[Keys.CADETE_ID] }
    val cadeteNombre: Flow<String?> = context.dataStore.data.map { it[Keys.CADETE_NOMBRE] }
    /** Preferencia de aparencia — no es parte de la sesión (sobrevive al logout, es del dispositivo). */
    val tema: Flow<TemaApp> = context.dataStore.data.map { TemaApp.from(it[Keys.TEMA]) }

    suspend fun baseUrlSync(): String = baseUrl.first()
    suspend fun tokenSync(): String? = token.first()
    suspend fun cadeteIdSync(): String? = cadeteId.first()
    suspend fun isLoggedIn(): Boolean = !tokenSync().isNullOrBlank()

    suspend fun setTema(tema: TemaApp) {
        context.dataStore.edit { it[Keys.TEMA] = tema.name }
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.BASE_URL] = url.trim().trimEnd('/') }
    }

    suspend fun guardarSesion(token: String, username: String, cadeteId: String, cadeteNombre: String) {
        context.dataStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.USERNAME] = username
            it[Keys.CADETE_ID] = cadeteId
            it[Keys.CADETE_NOMBRE] = cadeteNombre
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.USERNAME)
            it.remove(Keys.CADETE_ID)
            it.remove(Keys.CADETE_NOMBRE)
        }
    }

    /**
     * Último aviso general mostrado como notificación pero todavía no visto en el banner
     * flotante de la app — se guarda en disco (no en memoria) porque el aviso llega por
     * WebSocket incluso con la app minimizada, y si el sistema mata el proceso antes de
     * que el cadete vuelva a abrirla, un estado solo en memoria se pierde y el mensaje no
     * se vuelve a ver en ningún lado (la notificación del sistema ya desapareció sola).
     */
    val avisoFlotantePendiente: Flow<String?> = context.dataStore.data.map { it[Keys.AVISO_FLOTANTE_PENDIENTE] }
    suspend fun avisoFlotantePendienteSync(): String? = avisoFlotantePendiente.first()

    suspend fun guardarAvisoFlotantePendiente(mensaje: String) {
        context.dataStore.edit { it[Keys.AVISO_FLOTANTE_PENDIENTE] = mensaje }
    }

    suspend fun limpiarAvisoFlotantePendiente() {
        context.dataStore.edit { it.remove(Keys.AVISO_FLOTANTE_PENDIENTE) }
    }
}
