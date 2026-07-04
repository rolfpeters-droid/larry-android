package nl.rolfpeters.larry.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.rolfpeters.larry.data.SettingsStore
import nl.rolfpeters.larry.tts.TtsController
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    ttsController: TtsController,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
) {
    var endpoint by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var selectedVoiceName by remember { mutableStateOf("") }
    var speechRate by remember { mutableFloatStateOf(SettingsStore.DEFAULT_SPEECH_RATE) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        endpoint = settingsStore.endpointUrl.first()
        token = settingsStore.bearerToken.first()
        selectedVoiceName = settingsStore.ttsVoiceName.first()
        speechRate = settingsStore.ttsSpeechRate.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Terug")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Larry draait op de Mac mini en is alleen bereikbaar via Tailscale. " +
                    "Zorg dat de Tailscale-app op dit toestel actief/ingelogd is voor je verbindt.",
            )

            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it; saved = false },
                label = { Text("Larry endpoint") },
                placeholder = { Text(SettingsStore.ENDPOINT_HINT) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                "Bijvoorbeeld de Tailscale-hostname (http://mac-mini-van-rolf:5050/larry) " +
                    "of het 100.x-adres. Geen slash aan het einde nodig.",
            )

            OutlinedTextField(
                value = token,
                onValueChange = { token = it; saved = false },
                label = { Text("Bearer-token (optioneel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Button(
                onClick = {
                    scope.launch {
                        settingsStore.setEndpointUrl(endpoint)
                        settingsStore.setBearerToken(token)
                        settingsStore.setTtsVoiceName(selectedVoiceName)
                        settingsStore.setTtsSpeechRate(speechRate)
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (saved) "Opgeslagen" else "Opslaan")
            }

            HorizontalDivider()

            Text("Spraak (tekst-naar-spraak)", style = MaterialTheme.typography.titleMedium)

            Column {
                Text("Spreeksnelheid: ${formatRate(speechRate)}x")
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it; saved = false },
                    valueRange = SettingsStore.MIN_SPEECH_RATE..SettingsStore.MAX_SPEECH_RATE,
                    steps = 5, // stapjes van 0.25 tussen 0.5 en 2.0
                )
            }

            Text(
                "Kies een stem. Android's stem-namen verklappen niet betrouwbaar of het een " +
                    "mannen- of vrouwenstem is -- gebruik de testknop om te luisteren.",
            )

            if (!ttsController.isReady.value) {
                Text("Stemmen laden...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (ttsController.availableVoices.value.isEmpty()) {
                Text(
                    "Geen extra stemmen gevonden voor deze taal -- systeem-default wordt gebruikt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    // Optie voor systeem-default (leeg = geen expliciete keuze)
                    VoiceRow(
                        label = "Systeem-default",
                        selected = selectedVoiceName.isBlank(),
                        onSelect = { selectedVoiceName = ""; saved = false },
                        onTest = null,
                    )
                    ttsController.availableVoices.value.forEach { voice ->
                        VoiceRow(
                            label = voice.name,
                            selected = selectedVoiceName == voice.name,
                            onSelect = { selectedVoiceName = voice.name; saved = false },
                            onTest = {
                                ttsController.speakPreview(
                                    "Hoi, dit is Larry. Zo klink ik met deze stem.",
                                    voice,
                                    speechRate,
                                )
                            },
                        )
                    }
                }
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Chatgeschiedenis wissen")
            }

            Text(
                "Let op: geschiedenis wissen is definitief. Larry's eigen dagboek-samenvattingen " +
                    "(via SOP-002) blijven ongemoeid -- dit wist alleen de lokale kopie op dit toestel.",
            )
        }
    }
}

@Composable
private fun VoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onTest: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.weight(1f))
        if (onTest != null) {
            IconButton(onClick = onTest) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Test stem: $label")
            }
        }
    }
}

private fun formatRate(rate: Float): String = (round(rate * 100) / 100).toString()
