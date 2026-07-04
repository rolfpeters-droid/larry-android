package nl.rolfpeters.larry.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "larry_settings")

/**
 * Instellingen-opslag: endpoint (IP/hostname + poort), optioneel bearer-token, TTS-voorkeur.
 * Geen hardcoded IP -- alles hier is door de gebruiker instelbaar in het Settings-scherm,
 * conform de opdracht ("Endpoint configurable in Settings ... Geen hardcoded IP").
 */
class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_ENDPOINT = stringPreferencesKey("endpoint_url")
        private val KEY_BEARER_TOKEN = stringPreferencesKey("bearer_token")
        private val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")

        // Geen hardcoded productie-IP; dit is enkel een placeholder-hint die in het
        // Settings-scherm getoond wordt zolang de gebruiker nog niets heeft ingevuld.
        const val ENDPOINT_HINT = "http://100.x.x.x:5050/larry"
    }

    val endpointUrl: Flow<String> = context.dataStore.data.map { it[KEY_ENDPOINT] ?: "" }
    val bearerToken: Flow<String> = context.dataStore.data.map { it[KEY_BEARER_TOKEN] ?: "" }
    val ttsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_TTS_ENABLED] ?: false }

    suspend fun setEndpointUrl(url: String) {
        context.dataStore.edit { it[KEY_ENDPOINT] = url.trim().trimEnd('/') }
    }

    suspend fun setBearerToken(token: String) {
        context.dataStore.edit { it[KEY_BEARER_TOKEN] = token.trim() }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TTS_ENABLED] = enabled }
    }
}
