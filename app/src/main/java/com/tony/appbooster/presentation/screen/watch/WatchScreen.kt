package com.tony.appbooster.presentation.screen.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tony.appbooster.data.client.RemoteWatchAdbClient
import com.tony.appbooster.presentation.viewmodel.watch.WatchViewModel

@Composable
fun WatchScreen(
    viewModel: WatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WatchConnectionStatusCard(isConnected = uiState.isWatchConnected)
        }

        item {
            AdbConnectionStatusCard(
                state = uiState.adbConnectionState,
                onDisconnect = viewModel::disconnect
            )
        }

        // Only show instructions if not connected
        if (uiState.adbConnectionState !is RemoteWatchAdbClient.ConnectionState.Connected) {
            item {
                InstructionsCard()
            }
        }

        if (uiState.lastError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.lastError!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (uiState.lastMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = uiState.lastMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Only show setup inputs if not fully connected
        if (uiState.adbConnectionState !is RemoteWatchAdbClient.ConnectionState.Connected) {
            item {
                SetupSection(
                    uiState = uiState,
                    onUpdateIp = { viewModel.updateInputs(ip = it) },
                    onUpdatePairPort = { viewModel.updateInputs(pairPort = it) },
                    onUpdatePairCode = { viewModel.updateInputs(pairCode = it) },
                    onUpdateConnectPort = { viewModel.updateInputs(connectPort = it) },
                    onPair = viewModel::pair,
                    onConnect = viewModel::connect
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun WatchConnectionStatusCard(isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Watch,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Watch Link",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isConnected) "Connected via Wear OS" else "Not connected (Open app on watch)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AdbConnectionStatusCard(
    state: RemoteWatchAdbClient.ConnectionState,
    onDisconnect: () -> Unit
) {
    val (icon, color, text) = when (state) {
        is RemoteWatchAdbClient.ConnectionState.Connected ->
            Triple(Icons.Rounded.CheckCircle, MaterialTheme.colorScheme.primary, "ADB Connected")
        is RemoteWatchAdbClient.ConnectionState.Connecting ->
            Triple(Icons.Rounded.Info, MaterialTheme.colorScheme.tertiary, "Connecting...")
        is RemoteWatchAdbClient.ConnectionState.Disconnected ->
            Triple(Icons.Rounded.Error, MaterialTheme.colorScheme.outline, "ADB Disconnected")
        is RemoteWatchAdbClient.ConnectionState.Error ->
            Triple(Icons.Rounded.Error, MaterialTheme.colorScheme.error, "Connection Error")
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }

            if (state is RemoteWatchAdbClient.ConnectionState.Connected) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDisconnect) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How to Connect",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            InstructionStep(1, "Enable Developer Options", "Settings > System > About > Versions > Tap 'Build number' 7 times.")
            InstructionStep(2, "Enable Wireless Debugging", "Settings > Developer options > Wireless debugging > Toggle ON.")
            InstructionStep(3, "Connect", "Enter the IP address and Port shown on the watch screen.")

            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = "Note: If this is your first time connecting, you must pair the device first using the 'Pairing' section below.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(number: Int, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(24.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SetupSection(
    uiState: com.tony.appbooster.presentation.viewmodel.watch.WatchUiState,
    onUpdateIp: (String) -> Unit,
    onUpdatePairPort: (String) -> Unit,
    onUpdatePairCode: (String) -> Unit,
    onUpdateConnectPort: (String) -> Unit,
    onPair: () -> Unit,
    onConnect: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Connection Details",
            style = MaterialTheme.typography.titleLarge
        )


        OutlinedTextField(
            value = uiState.pairingIp,
            onValueChange = onUpdateIp,
            label = { Text("Watch IP Address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        HorizontalDivider()

        Text("Pairing (First Time Only)", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = uiState.pairingPort,
                onValueChange = onUpdatePairPort,
                label = { Text("Pairing Port") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = uiState.pairingCode,
                onValueChange = onUpdatePairCode,
                label = { Text("Pairing Code") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Button(
            onClick = onPair,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pair Device")
        }

        HorizontalDivider()

        Text("Connection", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = uiState.connectionPort,
            onValueChange = onUpdateConnectPort,
            label = { Text("Connection Port") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(
            onClick = onConnect,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }

        if (uiState.isBusy) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
