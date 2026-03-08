package com.tony.appbooster.presentation.screen.shizuku

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import com.tony.appbooster.presentation.screen.shizuku.components.CheckingStatusCard
import com.tony.appbooster.presentation.screen.shizuku.components.GrantPermissionCard
import com.tony.appbooster.presentation.screen.shizuku.components.InstallShizukuCard
import com.tony.appbooster.presentation.screen.shizuku.components.ReadyCard
import com.tony.appbooster.presentation.screen.shizuku.components.SetupProgressIndicator
import com.tony.appbooster.presentation.screen.shizuku.components.ShizukuHeroSection
import com.tony.appbooster.presentation.screen.shizuku.components.StartServiceCard
import com.tony.appbooster.presentation.tools.isTabletLayout
import com.tony.appbooster.presentation.ui.theme.AppBoosterTheme
import com.tony.appbooster.presentation.viewmodel.shizuku.ShizukuSetupStep
import com.tony.appbooster.presentation.viewmodel.shizuku.ShizukuSetupUiModel
import com.tony.appbooster.presentation.viewmodel.shizuku.ShizukuSetupViewModel

/**
 * Shizuku setup screen that guides users through enabling Shizuku for privileged operations.
 *
 * Follows Material Design 3 Expressive guidelines with a large hero section,
 * animated step transitions, clear progress indication, and prominent CTAs.
 * On tablet-class screens the layout splits into two columns: the left column
 * shows the hero illustration and progress indicator, the right column shows
 * the current step card.
 *
 * @param viewModel ViewModel providing UI state and event callbacks.
 * @param onSetupComplete Called once all Shizuku prerequisites are satisfied.
 */
@Composable
fun ShizukuSetupScreen(
    viewModel: ShizukuSetupViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh state whenever the screen becomes visible again
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onResumed()
        }
    }

    // Navigate away after the success animation settles
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
    val setupStep = remember(uiState.shizukuState) { uiState.shizukuState.toSetupStep() }
    val isTablet = isTabletLayout()

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
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isTablet) {
            ShizukuSetupTabletLayout(
                uiState = uiState,
                setupStep = setupStep,
                onInstallClicked = onInstallClicked,
                onOpenShizukuClicked = onOpenShizukuClicked,
                onRequestPermissionClicked = onRequestPermissionClicked,
                onRefreshClicked = onRefreshClicked,
                modifier = Modifier.padding(padding)
            )
        } else {
            ShizukuSetupPhoneLayout(
                uiState = uiState,
                setupStep = setupStep,
                onInstallClicked = onInstallClicked,
                onOpenShizukuClicked = onOpenShizukuClicked,
                onRequestPermissionClicked = onRequestPermissionClicked,
                onRefreshClicked = onRefreshClicked,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/**
 * Phone layout: hero, progress indicator, and step card stacked in a single scrollable column.
 */
@Composable
private fun ShizukuSetupPhoneLayout(
    uiState: ShizukuSetupUiModel,
    setupStep: ShizukuSetupStep,
    onInstallClicked: () -> Unit,
    onOpenShizukuClicked: () -> Unit,
    onRequestPermissionClicked: () -> Unit,
    onRefreshClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ShizukuHeroSection(step = setupStep)
        SetupProgressIndicator(step = setupStep)
        ShizukuStepCardAnimated(
            uiState = uiState,
            setupStep = setupStep,
            onInstallClicked = onInstallClicked,
            onOpenShizukuClicked = onOpenShizukuClicked,
            onRequestPermissionClicked = onRequestPermissionClicked,
            onRefreshClicked = onRefreshClicked
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Tablet two-column layout:
 * - Left column (45%): hero illustration centred vertically + progress indicator pinned to bottom.
 * - Right column (55%): animated step card scrollable independently.
 */
@Composable
private fun ShizukuSetupTabletLayout(
    uiState: ShizukuSetupUiModel,
    setupStep: ShizukuSetupStep,
    onInstallClicked: () -> Unit,
    onOpenShizukuClicked: () -> Unit,
    onRequestPermissionClicked: () -> Unit,
    onRefreshClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Left column: Hero + Progress ────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .padding(start = 32.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero section centred in the remaining vertical space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ShizukuHeroSection(step = setupStep)
            }

            // Progress indicator pinned to the bottom of the left column
            SetupProgressIndicator(step = setupStep)
            Spacer(Modifier.height(8.dp))
        }

        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // ── Right column: Step card ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 32.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.Top
        ) {
            ShizukuStepCardAnimated(
                uiState = uiState,
                setupStep = setupStep,
                onInstallClicked = onInstallClicked,
                onOpenShizukuClicked = onOpenShizukuClicked,
                onRequestPermissionClicked = onRequestPermissionClicked,
                onRefreshClicked = onRefreshClicked
            )
        }
    }
}

/**
 * Animated step card that transitions between wizard steps with a slide+fade effect.
 * Shared between phone and tablet layouts.
 *
 * @param uiState Current setup UI state.
 * @param setupStep Current wizard step derived from [uiState].
 */
@Composable
private fun ShizukuStepCardAnimated(
    uiState: ShizukuSetupUiModel,
    setupStep: ShizukuSetupStep,
    onInstallClicked: () -> Unit,
    onOpenShizukuClicked: () -> Unit,
    onRequestPermissionClicked: () -> Unit,
    onRefreshClicked: () -> Unit
) {
    AnimatedContent(
        targetState = setupStep,
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
}

/** Maps the current [ShizukuState] to the visual step shown in the setup wizard. */
private fun ShizukuState.toSetupStep(): ShizukuSetupStep {
    return when (this) {
        ShizukuState.NotInstalled -> ShizukuSetupStep.INSTALL_SHIZUKU
        ShizukuState.NotRunning -> ShizukuSetupStep.START_SERVICE
        ShizukuState.PermissionRequired -> ShizukuSetupStep.GRANT_PERMISSION
        ShizukuState.Ready -> ShizukuSetupStep.READY
        is ShizukuState.Error -> ShizukuSetupStep.CHECK_STATUS
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "Install – Light")
@Preview(name = "Install – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShizukuSetupInstallPreview() {
    AppBoosterTheme {
        ShizukuSetupContent(
            uiState = ShizukuSetupUiModel(shizukuState = ShizukuState.NotInstalled),
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
            uiState = ShizukuSetupUiModel(shizukuState = ShizukuState.NotRunning),
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
            uiState = ShizukuSetupUiModel(shizukuState = ShizukuState.PermissionRequired),
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
            uiState = ShizukuSetupUiModel(shizukuState = ShizukuState.Ready),
            onInstallClicked = {},
            onOpenShizukuClicked = {},
            onRequestPermissionClicked = {},
            onRefreshClicked = {}
        )
    }
}

