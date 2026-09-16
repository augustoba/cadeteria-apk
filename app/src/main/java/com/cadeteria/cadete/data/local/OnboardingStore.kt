package com.cadeteria.cadete.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.onboardingDataStore by preferencesDataStore(name = "cadete_onboarding")

/**
 * Un solo booleano: si ya se mostró el tutorial de bienvenida (mejora 2026-09-16) — se
 * guarda en el dispositivo, no en la cuenta, así que si el cadete cambia de celular lo
 * vuelve a ver una vez (no es grave, dura unos segundos y no bloquea nada).
 */
class OnboardingStore(private val context: Context) {
    private val keyVisto = booleanPreferencesKey("onboarding_visto")

    suspend fun visto(): Boolean = context.onboardingDataStore.data.first()[keyVisto] ?: false

    suspend fun marcarVisto() {
        context.onboardingDataStore.edit { prefs -> prefs[keyVisto] = true }
    }
}
