package nl.rolfpeters.larry

import android.app.Application
import nl.rolfpeters.larry.data.ChatDatabase
import nl.rolfpeters.larry.data.LarryApiClient
import nl.rolfpeters.larry.data.SettingsStore
import nl.rolfpeters.larry.tts.TtsController

/**
 * Application-scope singletons. Fase 1/POC: geen dependency-injection framework (Hilt/Koin),
 * gewoon handmatige lazy-init hier -- past bij "geen overengineering" uit de scope.
 */
class LarryApplication : Application() {

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val database: ChatDatabase by lazy { ChatDatabase.build(this) }
    val apiClient: LarryApiClient by lazy { LarryApiClient(settingsStore) }

    // TTS-engine leeft op Application-niveau i.p.v. Activity-niveau, zodat de stem-lijst en
    // ingestelde voorkeuren beschikbaar zijn vanuit zowel het Chat- als Instellingen-scherm.
    val ttsController: TtsController by lazy { TtsController(this, settingsStore) }
}
