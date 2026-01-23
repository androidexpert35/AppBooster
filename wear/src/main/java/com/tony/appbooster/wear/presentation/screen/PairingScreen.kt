package com.tony.appbooster.wear.presentation.screen

import android.app.RemoteInput
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.input.RemoteInputIntentHelper
import com.tony.appbooster.wear.domain.model.AdbConnectionState
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import com.tony.appbooster.wear.presentation.model.PairingUiState
import com.tony.appbooster.wear.presentation.viewmodel.PairingViewModel

private const val KEY_PAIRING_INFO = "pairing_info"
private const val KEY_CONNECTION_PORT = "connection_port"

// Wizard steps
private const val STEP_INSTRUCTIONS = 0
private const val STEP_ENTER_PAIRING = 1
private const val STEP_ENTER_CONNECTION = 2

/**
 * Screen for pairing with the local ADB daemon.
 *
 * Uses a wizard-style approach because the pairing dialog on Wear OS
 * closes when switching apps. The user must memorize or note down
 * the pairing port and code before returning to this app.
 *
 * @param viewModel The ViewModel for this screen.
 * @param onPairingComplete Callback when pairing and connection succeed.
 */
@Composable
fun PairingScreen(
    viewModel: PairingViewModel = hiltViewModel(),
    repository: WearAdbRepository,
    onPairingComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionState by repository.connectionState.collectAsStateWithLifecycle()

    // Navigate away when connected
    LaunchedEffect(connectionState) {
        if (connectionState == AdbConnectionState.Connected) {
            onPairingComplete()
        }
    }

    // Track wizard step
    var currentStep by remember { mutableIntStateOf(STEP_INSTRUCTIONS) }

    // Auto-advance when pairing succeeds
    LaunchedEffect(uiState.pairingSuccess) {
        if (uiState.pairingSuccess) {
            currentStep = STEP_ENTER_CONNECTION
        }
    }

    PairingWizard(
        uiState = uiState,
        currentStep = currentStep,
        onStepChange = { currentStep = it },
        onPairingInfoEntered = { port, code ->
            viewModel.onPairingPortChanged(port)
            viewModel.onPairingCodeChanged(code)
        },
        onConnectionPortChanged = viewModel::onConnectionPortChanged,
        onPair = viewModel::pair,
        onConnect = viewModel::connect,
        onSkipPairing = {
            // Skip to connection step (for already-paired devices)
            viewModel.markAsPaired()
            currentStep = STEP_ENTER_CONNECTION
        }
    )
}

@Composable
private fun PairingWizard(
    uiState: PairingUiState,
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    onPairingInfoEntered: (port: String, code: String) -> Unit,
    onConnectionPortChanged: (String) -> Unit,
    onPair: () -> Unit,
    onConnect: () -> Unit,
    onSkipPairing: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    // Combined launcher for port:code input
    val pairingInfoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results = RemoteInput.getResultsFromIntent(data)
            val input = results?.getCharSequence(KEY_PAIRING_INFO)?.toString() ?: ""
            // Parse "port:code" or "port code" format
            val parts = input.replace(":", " ").replace(",", " ").split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                onPairingInfoEntered(parts[0], parts[1])
            } else if (parts.size == 1 && parts[0].length > 6) {
                // Assume first 5 chars are port, rest is code
                val port = parts[0].take(5)
                val code = parts[0].drop(5).take(6)
                onPairingInfoEntered(port, code)
            }
        }
    }

    // Connection port launcher
    val connectionPortLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results = RemoteInput.getResultsFromIntent(data)
            val port = results?.getCharSequence(KEY_CONNECTION_PORT)?.toString() ?: ""
            onConnectionPortChanged(port)
        }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(20.dp)) }

        when (currentStep) {
            STEP_INSTRUCTIONS -> {
                item {
                    Text(
                        text = "Setup",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Text(
                        text = "Step 1 of 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Text(
                        text = "⚠️ Read carefully!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    Text(
                        text = "The pairing code will disappear when you return here. You must MEMORIZE or WRITE DOWN both values:",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                item {
                    Text(
                        text = "1️⃣ Pairing Port (5 digits)\n2️⃣ Pairing Code (6 digits)",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Text(
                        text = "Go to:\nSettings → Developer Options → Wireless Debugging → Pair new device",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    Button(
                        onClick = { onStepChange(STEP_ENTER_PAIRING) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("I have the values")
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = onSkipPairing
                    ) {
                        Text(
                            text = "Already paired via PC",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            STEP_ENTER_PAIRING -> {
                item {
                    Text(
                        text = "Enter Pairing Info",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Text(
                        text = "Step 1 of 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Text(
                        text = "Enter: PORT CODE\n(e.g., 37845 482916)",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Combined input button
                item {
                    Button(
                        onClick = {
                            val intent = createRemoteInputIntent(
                                key = KEY_PAIRING_INFO,
                                label = "Port and Code (e.g., 37845 482916)"
                            )
                            pairingInfoLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Text(
                            text = if (uiState.pairingPort.isBlank())
                                "Enter Port & Code"
                            else
                                "${uiState.pairingPort} / ${uiState.pairingCode}",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Pair button
                item {
                    Button(
                        onClick = onPair,
                        enabled = uiState.canPair,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isPairing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Pair")
                        }
                    }
                }

                // Back button
                item {
                    TextButton(onClick = { onStepChange(STEP_INSTRUCTIONS) }) {
                        Text("Back")
                    }
                }

                // Error
                uiState.errorMessage?.let { error ->
                    item {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            STEP_ENTER_CONNECTION -> {
                item {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                item {
                    Text(
                        text = "Paired!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Text(
                        text = "Step 2 of 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Text(
                        text = "Now enter the CONNECTION port from the main Wireless Debugging screen (not the pairing port!)",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Connection port button
                item {
                    Button(
                        onClick = {
                            val intent = createRemoteInputIntent(
                                key = KEY_CONNECTION_PORT,
                                label = "Connection Port"
                            )
                            connectionPortLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Text(
                            text = if (uiState.connectionPort.isBlank())
                                "Enter Connection Port"
                            else
                                "Port: ${uiState.connectionPort}",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Connect button
                item {
                    Button(
                        onClick = onConnect,
                        enabled = uiState.canConnect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Connect")
                        }
                    }
                }

                // Error
                uiState.errorMessage?.let { error ->
                    item {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Retry pairing option
                    item {
                        TextButton(onClick = { onStepChange(STEP_INSTRUCTIONS) }) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Retry pairing",
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/**
 * Creates a RemoteInput intent for Wear OS text input.
 */
private fun createRemoteInputIntent(
    key: String,
    label: String
): Intent {
    val remoteInput = RemoteInput.Builder(key)
        .setLabel(label)
        .build()

    return RemoteInputIntentHelper.createActionRemoteInputIntent().apply {
        RemoteInputIntentHelper.putRemoteInputsExtra(this, listOf(remoteInput))
    }
}

