package com.tony.appbooster.presentation.screen.watch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tony.appbooster.data.client.RemoteWatchAdbClient
import com.tony.appbooster.domain.model.common.OptimizationResult
import com.tony.appbooster.presentation.viewmodel.watch.WatchUiState
import com.tony.appbooster.presentation.viewmodel.watch.WatchViewModel

/**
 * Screen for managing watch connection and remote optimization.
 *
 * Allows users to connect to their Wear OS watch via wireless ADB
 * and run optimization commands remotely from the phone.
 *
 * @param viewModel ViewModel for watch management.
 */
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

        // Show optimization controls when connected
        if (uiState.adbConnectionState is RemoteWatchAdbClient.ConnectionState.Connected) {
            item {
                OptimizationControlCard(
                    uiState = uiState,
                    onStart = viewModel::startWatchOptimization,
                    onCancel = viewModel::cancelOptimization,
                    onToggleMode = viewModel::toggleOptimizationMode
                )
            }

            // Show logs when optimization is running or complete
            if (uiState.commandLogs.isNotEmpty()) {
                item {
                    LogOutputCard(logs = uiState.commandLogs)
                }
            }
        }

        // Show instructions only if not connected
        if (uiState.adbConnectionState !is RemoteWatchAdbClient.ConnectionState.Connected) {
            item {
                InstructionsCard()
            }
        }

        // Error message
        uiState.lastError?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Success message
        uiState.lastMessage?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Connection setup inputs when not connected
        if (uiState.adbConnectionState !is RemoteWatchAdbClient.ConnectionState.Connected) {
            item {
                SetupSection(
                    uiState = uiState,
                    onUpdateIp = { viewModel.updateInputs(ip = it) },
                    onUpdatePairPort = { viewModel.updateInputs(pairPort = it) },
                    onUpdatePairCode = { viewModel.updateInputs(pairCode = it) },
                    onUpdateConnectPort = { viewModel.updateInputs(connectPort = it) },
                    onPair = viewModel::pair,
                    onConnect = viewModel::connect,
                    onImportKeys = viewModel::showKeyImportDialog
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Key Import Dialog
    if (uiState.showKeyImportDialog) {
        KeyImportDialog(
            privateKey = uiState.privateKeyInput,
            publicKey = uiState.publicKeyInput,
            onPrivateKeyChange = { viewModel.updateKeyInputs(privateKey = it) },
            onPublicKeyChange = { viewModel.updateKeyInputs(publicKey = it) },
            onImport = viewModel::importKeys,
            onDismiss = viewModel::hideKeyImportDialog,
            isBusy = uiState.isBusy
        )
    }
}

/**
 * Card displaying optimization controls and progress.
 */
@Composable
fun OptimizationControlCard(
    uiState: WatchUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onToggleMode: () -> Unit
) {
    val isRunning = uiState.optimizationProgress.isRunning
    val isComplete = uiState.optimizationProgress.result == OptimizationResult.Completed
    val progress by animateFloatAsState(
        targetValue = uiState.optimizationProgress.progress,
        animationSpec = tween(300),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isComplete -> MaterialTheme.colorScheme.primaryContainer
                isRunning -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isComplete -> Icons.Rounded.CheckCircle
                        isRunning -> Icons.Rounded.Refresh
                        else -> Icons.Rounded.PlayArrow
                    },
                    contentDescription = null,
                    tint = when {
                        isComplete -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when {
                        isComplete -> "Optimization Complete"
                        isRunning -> "Optimizing Watch..."
                        else -> "Watch Optimization"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Progress section
            AnimatedVisibility(
                visible = isRunning,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.optimizationProgress.currentAppPackage,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${uiState.optimizationProgress.processedCount}/${uiState.optimizationProgress.totalCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Complete message
            AnimatedVisibility(visible = isComplete) {
                Text(
                    text = "${uiState.optimizationProgress.totalCount} apps optimized successfully!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Mode selector (only when not running)
            if (!isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mode:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FilledTonalButton(onClick = onToggleMode) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.selectedOptimizationType.name.replace("_", " "))
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRunning) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isComplete) "Optimize Again" else "Start Optimization")
                    }
                }
            }
        }
    }
}

/**
 * Card displaying command log output.
 */
@Composable
fun LogOutputCard(logs: List<String>) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs added
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Log Output",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.shapes.small
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            log.startsWith("✓") -> MaterialTheme.colorScheme.primary
                            log.startsWith("Failed") || log.startsWith("Error") ->
                                MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
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
                    text = "Watch Optimization",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "Connect to your watch via wireless ADB to optimize apps remotely.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "On your watch:",
                style = MaterialTheme.typography.titleSmall
            )

            InstructionStep(1, "Enable Developer Options", "Settings → System → About → Tap 'Build number' 7 times")
            InstructionStep(2, "Enable Wireless Debugging", "Developer options → Wireless debugging → ON")
            InstructionStep(3, "Pair (one-time)", "Tap 'Pair new device' to get the pairing info")

            Spacer(modifier = Modifier.height(4.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚠️ Recommended: Pair from PC",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "For reliable pairing, use a PC:\n" +
                                "1. Run: adb pair <IP>:<PAIR_PORT>\n" +
                                "2. Enter the 6-digit code\n" +
                                "3. Then use 'Connect' below with the connection port",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            InstructionStep(4, "Connect from here", "Enter IP and connection port below, then tap 'Connect'")

            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "💡 After connecting once, the watch will remember this phone for future connections!",
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
    uiState: WatchUiState,
    onUpdateIp: (String) -> Unit,
    onUpdatePairPort: (String) -> Unit,
    onUpdatePairCode: (String) -> Unit,
    onUpdateConnectPort: (String) -> Unit,
    onPair: () -> Unit,
    onConnect: () -> Unit,
    onImportKeys: () -> Unit
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

        // Alternative: Import keys from PC
        OutlinedButton(
            onClick = onImportKeys,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Keys from PC")
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

/**
 * Dialog for importing ADB keys from PC.
 */
@Composable
fun KeyImportDialog(
    privateKey: String,
    publicKey: String,
    onPrivateKeyChange: (String) -> Unit,
    onPublicKeyChange: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
    isBusy: Boolean
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import ADB Keys from PC") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Copy your PC's ADB keys to allow the phone to connect to devices paired with your PC.\n\n" +
                            "On your PC, find these files:\n" +
                            "• Windows: C:\\Users\\<name>\\.android\\adbkey\n" +
                            "• Mac/Linux: ~/.android/adbkey",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = privateKey,
                    onValueChange = onPrivateKeyChange,
                    label = { Text("Private Key (adbkey content)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 8,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )

                OutlinedTextField(
                    value = publicKey,
                    onValueChange = onPublicKeyChange,
                    label = { Text("Public Key (adbkey.pub content)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = !isBusy && privateKey.isNotBlank() && publicKey.isNotBlank()
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

