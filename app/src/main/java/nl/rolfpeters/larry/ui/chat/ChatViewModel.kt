package nl.rolfpeters.larry.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.rolfpeters.larry.data.ChatDao
import nl.rolfpeters.larry.data.ChatMessageEntity
import nl.rolfpeters.larry.data.LarryApiClient
import nl.rolfpeters.larry.data.LarryResult
import nl.rolfpeters.larry.data.SettingsStore

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val ttsEnabled: Boolean = false,
)

/**
 * Fase 1: geen streaming (backend levert dat niet, zie scope.md). De UI toont een
 * "denkt na..."-status tot het volledige antwoord binnen is.
 *
 * Geschiedenis wordt volledig lokaal beheerd (Room) en bij elke request in zijn geheel
 * meegestuurd aan Larry, want de backend heeft zelf geen sessie/geheugen.
 */
class ChatViewModel(
    private val chatDao: ChatDao,
    private val apiClient: LarryApiClient,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatDao.observeAll().collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
        viewModelScope.launch {
            settingsStore.ttsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(ttsEnabled = enabled)
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isSending) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)

            chatDao.insert(ChatMessageEntity(role = "user", content = trimmed))

            // Volledige geschiedenis (incl. het bericht dat we net invoegden) meesturen --
            // Larry heeft geen server-side geheugen.
            val fullHistory = chatDao.getAll()

            when (val result = apiClient.sendMessages(fullHistory)) {
                is LarryResult.Success -> {
                    chatDao.insert(ChatMessageEntity(role = "assistant", content = result.reply))
                }
                is LarryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            }

            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearHistory() {
        viewModelScope.launch { chatDao.clearAll() }
    }

    fun toggleTts(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setTtsEnabled(enabled) }
    }

    suspend fun hasEndpointConfigured(): Boolean = settingsStore.endpointUrl.first().isNotBlank()
}
