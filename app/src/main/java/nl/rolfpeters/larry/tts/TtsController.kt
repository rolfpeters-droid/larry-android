package nl.rolfpeters.larry.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.first
import nl.rolfpeters.larry.data.SettingsStore
import java.util.Locale

/**
 * Beheert de Android TextToSpeech-engine: initialisatie, stem-lijst opvragen, en toepassen
 * van de door de gebruiker gekozen stem + spreeksnelheid uit SettingsStore.
 *
 * Android's Voice-API heeft geen betrouwbaar "gender"-veld dat op alle TTS-engines werkt --
 * daarom laten we de gebruiker zelf luisteren en kiezen via een test-knop in Instellingen,
 * in plaats van te gokken op basis van voice-namen.
 */
class TtsController(
    context: Context,
    private val settingsStore: SettingsStore,
) {
    private var tts: TextToSpeech? = null
    val isReady = mutableStateOf(false)
    val availableVoices = mutableStateOf<List<Voice>>(emptyList())

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                // Alleen stemmen in de huidige systeemtaal tonen -- een volledige lijst van
                // alle talen door elkaar is onoverzichtelijk in de UI.
                availableVoices.value = tts?.voices
                    ?.filter { it.locale.language == Locale.getDefault().language }
                    ?.sortedBy { it.name }
                    .orEmpty()
                isReady.value = true
            }
        }
    }

    /** Past de opgeslagen voorkeuren (stem + snelheid) toe op de gegeven TTS-instantie. */
    suspend fun applyStoredPreferences() {
        val voiceName = settingsStore.ttsVoiceName.first()
        val rate = settingsStore.ttsSpeechRate.first()
        applyVoice(voiceName)
        tts?.setSpeechRate(rate)
    }

    private fun applyVoice(voiceName: String) {
        if (voiceName.isBlank()) return // leeg = systeem-default, niets te doen
        val voice = availableVoices.value.find { it.name == voiceName }
        if (voice != null) {
            tts?.voice = voice
        }
    }

    /** Spreek tekst uit met een specifieke stem (voor de "Test"-knop in Instellingen) zonder
     *  de opgeslagen voorkeur te wijzigen. */
    fun speakPreview(text: String, voice: Voice, rate: Float) {
        tts?.voice = voice
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "larry_voice_preview")
    }

    /** Spreek een echt Larry-antwoord uit met de opgeslagen voorkeuren. */
    suspend fun speak(text: String) {
        applyStoredPreferences()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "larry_reply")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
