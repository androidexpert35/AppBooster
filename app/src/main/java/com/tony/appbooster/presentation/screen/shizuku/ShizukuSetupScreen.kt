package com.tony.appbooster.presentation.screen.shizuku

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import com.tony.appbooster.presentation.ui.theme.AppBoosterTheme
import com.tony.appbooster.presentation.viewmodel.shizuku.ShizukuSetupStep
import com.tony.appbooster.presentation.viewmodel.shizuku.ShizukuSetupUiModel
import com.tony.appbooster.presentation.viewmodel.shizuku.ShizukuSetupViewModel

/**
 * Shizuku setup screen that guides users through enabling Shizuku for privileged operations.
 *
 * Follows Material Design 3 Expressive guidelines with:
 * - Large, expressive hero section
 * - Clear step-by-step progress indication
 * - Smooth animations between states
 * - Prominent CTAs with appropriate emphasis
 */
@Composable
fun ShizukuSetupScreen(
    viewModel: ShizukuSetupViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh state when screen becomes visible
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onResumed()
        }
    }

    // Navigate when ready
    LaunchedEffect(uiState.shizukuState) {
        if (uiState.shizukuState == ShizukuState.Ready) {
            // Small delay for the success animation
            kotlinx.coroutines.delay(800)
            onSetupComplete()
        }
    }

    ShizukuSetupContent(
        uiState = uiState,
        onInstallClicked = viewModel::onInstallShizukuClicked,
        onOpenShizukuClicked = viewModel::onOpenShizukuClicked,
        onRequestPermissionClicked = viewModel::onRequestPermissionClicked,
        onRefreshClicked = viewModel::refreshState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShizukuSetupContent(
    uiState: ShizukuSetupUiModel,
    onInstallClicked: () -> Unit,
    onOpenShizukuClicked: () -> Unit,
    onRequestPermissionClicked: () -> Unit,
    onRefreshClicked: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shizuku_setup_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onRefreshClicked,
                        enabled = !uiState.isCheckingState
                    ) {
                        if (uiState.isCheckingState) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.shizuku_refresh_status_cd)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section
            ShizukuHeroSection(step = uiState.setupStep)

            // Progress Indicator
            SetupProgressIndicator(step = uiState.setupStep)

            // Step Content
            AnimatedContent(
                targetState = uiState.setupStep,
                transitionSpec = {
                    (slideInVertically { it / 4 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 4 } + fadeOut())
                },
                label = "step_content"
            ) { step ->
                when (step) {
                    ShizukuSetupStep.CHECK_STATUS -> CheckingStatusCard(
                        isChecking = uiState.isCheckingState,
                        error = (uiState.shizukuState as? ShizukuState.Error)?.message,
                        onRetry = onRefreshClicked
                    )
                    ShizukuSetupStep.INSTALL_SHIZUKU -> InstallShizukuCard(
                        onInstallClicked = onInstallClicked
                    )
                    ShizukuSetupStep.START_SERVICE -> StartServiceCard(
                        onOpenShizukuClicked = onOpenShizukuClicked
                    )
                    ShizukuSetupStep.GRANT_PERMISSION -> GrantPermissionCard(
                        onGrantClicked = onRequestPermissionClicked,
                        isRequesting = uiState.isCheckingState
                    )
                    ShizukuSetupStep.READY -> ReadyCard()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShizukuHeroSection(step: ShizukuSetupStep) {
    val icon = when (step) {
        ShizukuSetupStep.CHECK_STATUS -> Icons.Rounded.Terminal
        ShizukuSetupStep.INSTALL_SHIZUKU -> Icons.Rounded.Download
        ShizukuSetupStep.START_SERVICE -> Icons.Rounded.PlayArrow
        ShizukuSetupStep.GRANT_PERMISSION -> Icons.Rounded.Key
        ShizukuSetupStep.READY -> Icons.Rounded.CheckCircle
    }

    val title = when (step) {
        ShizukuSetupStep.CHECK_STATUS -> stringResource(R.string.shizuku_hero_checking_title)
        ShizukuSetupStep.INSTALL_SHIZUKU -> stringResource(R.string.shizuku_hero_install_title)
        ShizukuSetupStep.START_SERVICE -> stringResource(R.string.shizuku_hero_start_title)
        ShizukuSetupStep.GRANT_PERMISSION -> stringResource(R.string.shizuku_hero_permission_title)
        ShizukuSetupStep.READY -> stringResource(R.string.shizuku_hero_ready_title)
    }

    val subtitle = when (step) {
        ShizukuSetupStep.CHECK_STATUS -> stringResource(R.string.shizuku_hero_checking_subtitle)
        ShizukuSetupStep.INSTALL_SHIZUKU -> stringResource(R.string.shizuku_hero_install_subtitle)
        ShizukuSetupStep.START_SERVICE -> stringResource(R.string.shizuku_hero_start_subtitle)
        ShizukuSetupStep.GRANT_PERMISSION -> stringResource(R.string.shizuku_hero_permission_subtitle)
        ShizukuSetupStep.READY -> stringResource(R.string.shizuku_hero_ready_subtitle)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Animated icon with background
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (scaleIn(spring(stiffness = Spring.StiffnessLow)) + fadeIn()) togetherWith
                        (scaleOut() + fadeOut())
            },
            label = "hero_icon"
        ) { currentStep ->
            val isReady = currentStep == ShizukuSetupStep.READY
            val containerColor = if (isReady) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
            val contentColor = if (isReady) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = contentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "hero_title"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = subtitle,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "hero_subtitle"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SetupProgressIndicator(step: ShizukuSetupStep) {
    val progress by animateFloatAsState(
        targetValue = when (step) {
            ShizukuSetupStep.CHECK_STATUS -> 0f
            ShizukuSetupStep.INSTALL_SHIZUKU -> 0.25f
            ShizukuSetupStep.START_SERVICE -> 0.5f
            ShizukuSetupStep.GRANT_PERMISSION -> 0.75f
            ShizukuSetupStep.READY -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.shizuku_setup_progress_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            strokeCap = StrokeCap.Round,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun SetupStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onActionClicked: () -> Unit,
    isLoading: Boolean = false,
    secondaryActionLabel: String? = null,
    onSecondaryActionClicked: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (secondaryActionLabel != null && onSecondaryActionClicked != null) {
                    OutlinedButton(
                        onClick = onSecondaryActionClicked,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(secondaryActionLabel)
                    }
                }

                Button(
                    onClick = onActionClicked,
                    modifier = if (secondaryActionLabel != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(actionLabel)
                    if (!isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckingStatusCard(
    isChecking: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.shizuku_checking_status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (error != null) {
                Text(
                    text = stringResource(R.string.shizuku_checking_error_with_reason, error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
    }
}

@Composable
private fun InstallShizukuCard(onInstallClicked: () -> Unit) {
    SetupStepCard(
        icon = Icons.Rounded.Download,
        title = stringResource(R.string.shizuku_hero_install_title),
        description = stringResource(R.string.shizuku_install_description),
        actionLabel = stringResource(R.string.shizuku_download_action),
        onActionClicked = onInstallClicked
    )
}

@Composable
private fun StartServiceCard(onOpenShizukuClicked: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = stringResource(R.string.shizuku_start_service_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(R.string.shizuku_start_service_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ADB Method Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.shizuku_method_adb_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = stringResource(R.string.shizuku_method_adb_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shizuku_method_adb_command),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Root Method Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.shizuku_method_root_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = stringResource(R.string.shizuku_method_root_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onOpenShizukuClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_open_shizuku))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun GrantPermissionCard(
    onGrantClicked: () -> Unit,
    isRequesting: Boolean
) {
    SetupStepCard(
        icon = Icons.Rounded.Key,
        title = stringResource(R.string.shizuku_hero_permission_title),
        description = stringResource(R.string.shizuku_grant_description),
        actionLabel = if (isRequesting) stringResource(R.string.shizuku_grant_requesting) else stringResource(R.string.shizuku_hero_permission_title),
        onActionClicked = onGrantClicked,
        isLoading = isRequesting
    )
}

@Composable
private fun ReadyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Rocket,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = stringResource(R.string.shizuku_ready_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = stringResource(R.string.shizuku_ready_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.shizuku_ready_redirecting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// Previews
@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShizukuSetupInstallPreview() {
    AppBoosterTheme {
        ShizukuSetupContent(
            uiState = ShizukuSetupUiModel(
                shizukuState = ShizukuState.NotInstalled,
                setupStep = ShizukuSetupStep.INSTALL_SHIZUKU
            ),
            onInstallClicked = {},
            onOpenShizukuClicked = {},
            onRequestPermissionClicked = {},
            onRefreshClicked = {}
        )
    }
}

@Preview(name = "Start Service")
@Composable
private fun ShizukuSetupStartServicePreview() {
    AppBoosterTheme {
        ShizukuSetupContent(
            uiState = ShizukuSetupUiModel(
                shizukuState = ShizukuState.NotRunning,
                setupStep = ShizukuSetupStep.START_SERVICE
            ),
            onInstallClicked = {},
            onOpenShizukuClicked = {},
            onRequestPermissionClicked = {},
            onRefreshClicked = {}
        )
    }
}

@Preview(name = "Grant Permission")
@Composable
private fun ShizukuSetupPermissionPreview() {
    AppBoosterTheme {
        ShizukuSetupContent(
            uiState = ShizukuSetupUiModel(
                shizukuState = ShizukuState.PermissionRequired,
                setupStep = ShizukuSetupStep.GRANT_PERMISSION
            ),
            onInstallClicked = {},
            onOpenShizukuClicked = {},
            onRequestPermissionClicked = {},
            onRefreshClicked = {}
        )
    }
}

@Preview(name = "Ready")
@Composable
private fun ShizukuSetupReadyPreview() {
    AppBoosterTheme {
        ShizukuSetupContent(
            uiState = ShizukuSetupUiModel(
                shizukuState = ShizukuState.Ready,
                setupStep = ShizukuSetupStep.READY
            ),
            onInstallClicked = {},
            onOpenShizukuClicked = {},
            onRequestPermissionClicked = {},
            onRefreshClicked = {}
        )
    }
}
