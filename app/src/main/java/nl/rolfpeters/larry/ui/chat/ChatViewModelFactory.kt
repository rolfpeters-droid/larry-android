package nl.rolfpeters.larry.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import nl.rolfpeters.larry.data.ChatDao
import nl.rolfpeters.larry.data.LarryApiClient
import nl.rolfpeters.larry.data.SettingsStore

/** Handmatige ViewModel-factory -- geen Hilt/Koin, conform "geen overengineering" (Fase 1). */
class ChatViewModelFactory(
    private val chatDao: ChatDao,
    private val apiClient: LarryApiClient,
    private val settingsStore: SettingsStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(chatDao, apiClient, settingsStore) as T
    }
}
