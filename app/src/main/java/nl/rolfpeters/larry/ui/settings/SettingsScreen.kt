package nl.rolfpeters.larry.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.rolfpeters.larry.data.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
) {
    var endpoint by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        endpoint = settingsStore.endpointUrl.first()
        token = settingsStore.bearerToken.first()
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
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (saved) "Opgeslagen" else "Opslaan")
            }

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
