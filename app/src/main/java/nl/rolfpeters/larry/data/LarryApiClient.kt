package nl.rolfpeters.larry.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class LarryResult {
    data class Success(val reply: String) : LarryResult()
    data class Failure(val message: String) : LarryResult()
}

/**
 * Client voor Larry's /larry/chat endpoint.
 *
 * Contract (geinventariseerd, zie scope.md):
 *   POST {endpoint}/chat
 *   body:     {"messages": [{"role": "user"|"assistant", "content": "..."}]}
 *   response: {"reply": "..."}  (200, synchroon -- GEEN streaming)
 *
 * Larry heeft geen server-side sessie/geheugen, dus we sturen bij elke aanroep de VOLLEDIGE
 * geschiedenis mee (zoals Larry zelf bevestigde tijdens de inventarisatie).
 */
class LarryApiClient(private val settingsStore: SettingsStore) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // Larry's antwoord is niet-streamend, kan even duren
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessages(history: List<ChatMessageEntity>): LarryResult = withContext(Dispatchers.IO) {
        val endpoint = settingsStore.endpointUrl.first()
        if (endpoint.isBlank()) {
            return@withContext LarryResult.Failure(
                "Geen endpoint ingesteld. Ga naar Instellingen en vul Larry's Tailscale-adres in."
            )
        }
        val token = settingsStore.bearerToken.first()

        val messagesJson = JSONArray()
        history.forEach { msg ->
            messagesJson.put(
                JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                }
            )
        }
        val bodyJson = JSONObject().apply { put("messages", messagesJson) }

        val requestBuilder = Request.Builder()
            .url("$endpoint/chat")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))

        if (token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorDetail = runCatching { JSONObject(bodyString).optString("error") }.getOrNull()
                    return@withContext LarryResult.Failure(
                        "Larry gaf een fout terug (HTTP ${response.code})" +
                            if (!errorDetail.isNullOrBlank()) ": $errorDetail" else ""
                    )
                }
                val reply = runCatching { JSONObject(bodyString).optString("reply") }.getOrNull()
                if (reply.isNullOrBlank()) {
                    LarryResult.Failure("Onverwacht antwoordformaat van Larry.")
                } else {
                    LarryResult.Success(reply)
                }
            }
        } catch (e: IOException) {
            LarryResult.Failure(
                "Kan Larry niet bereiken. Controleer of Tailscale actief is en het endpoint " +
                    "in Instellingen klopt. (${e.message ?: "netwerkfout"})"
            )
        }
    }
}
