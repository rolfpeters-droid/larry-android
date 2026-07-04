package nl.rolfpeters.larry

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nl.rolfpeters.larry.ui.chat.ChatScreen
import nl.rolfpeters.larry.ui.chat.ChatViewModel
import nl.rolfpeters.larry.ui.chat.ChatViewModelFactory
import nl.rolfpeters.larry.ui.settings.SettingsScreen
import nl.rolfpeters.larry.ui.theme.LarryTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var textToSpeech: TextToSpeech? = null
    private var lastSpokenMessageId: Long = -1

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* resultaat wordt bij de volgende voice-tap opnieuw gecheckt */ }

    private var onVoiceResult: ((String) -> Unit)? = null
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!text.isNullOrBlank()) {
            onVoiceResult?.invoke(text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }

        val app = application as LarryApplication

        setContent {
            LarryTheme {
                val navController = rememberNavController()
                var voiceInputResult by remember { mutableStateOf<String?>(null) }

                onVoiceResult = { text -> voiceInputResult = text }

                NavHost(navController = navController, startDestination = "chat") {
                    composable("chat") {
                        val viewModel: ChatViewModel = viewModel(
                            factory = ChatViewModelFactory(
                                chatDao = app.database.chatDao(),
                                apiClient = app.apiClient,
                                settingsStore = app.settingsStore,
                            ),
                        )
                        val uiState by viewModel.uiState.collectAsState()

                        // Spreek Larry's laatste antwoord uit als TTS aanstaat
                        LaunchedEffect(uiState.messages.size, uiState.ttsEnabled) {
                            val lastAssistantMessage = uiState.messages.lastOrNull { it.role == "assistant" }
                            if (uiState.ttsEnabled && lastAssistantMessage != null &&
                                lastAssistantMessage.id != lastSpokenMessageId
                            ) {
                                lastSpokenMessageId = lastAssistantMessage.id
                                speak(lastAssistantMessage.content)
                            }
                        }

                        ChatScreen(
                            viewModel = viewModel,
                            onOpenSettings = { navController.navigate("settings") },
                            onStartVoiceInput = { startVoiceInput() },
                            voiceInputResult = voiceInputResult,
                            onVoiceInputConsumed = { voiceInputResult = null },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settingsStore = app.settingsStore,
                            onBack = { navController.popBackStack() },
                            onClearHistory = {
                                val viewModel: ChatViewModel = viewModel(
                                    factory = ChatViewModelFactory(
                                        chatDao = app.database.chatDao(),
                                        apiClient = app.apiClient,
                                        settingsStore = app.settingsStore,
                                    ),
                                )
                                viewModel.clearHistory()
                            },
                        )
                    }
                }
            }
        }
    }

    private fun startVoiceInput() {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Spreek je bericht voor Larry in...")
        }
        runCatching { speechRecognizerLauncher.launch(intent) }
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "larry_reply")
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}
