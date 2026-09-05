package com.onigiri.spend.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onigiri.spend.data.ApiClient
import com.onigiri.spend.data.SessionStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    session: SessionStore,
    apiClient: ApiClient,
    showLogout: Boolean,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val storedUrl by session.baseUrl.collectAsState(initial = SessionStore.DEFAULT_BASE_URL)
    val storedCurrency by session.currency.collectAsState(initial = "JPY")
    var baseUrl by remember { mutableStateOf(storedUrl) }
    var currency by remember { mutableStateOf(storedCurrency) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(storedUrl) { baseUrl = storedUrl }
    LaunchedEffect(storedCurrency) { currency = storedCurrency }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; saved = false },
                label = { Text("API base URL") },
                supportingText = { Text("Emulator: http://10.0.2.2:8000  •  Device: http://<your-lan-ip>:8000") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it; saved = false },
                label = { Text("Default currency") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        session.setBaseUrl(baseUrl)
                        session.setCurrency(currency.ifBlank { "JPY" })
                        apiClient.invalidate()
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (saved) "Saved" else "Save")
            }
            if (showLogout) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            session.setToken(null)
                            apiClient.tokenSnapshot = null
                            onLoggedOut()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Log out")
                }
            }
        }
    }
}
