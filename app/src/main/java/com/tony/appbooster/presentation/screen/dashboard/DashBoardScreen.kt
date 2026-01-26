package com.tony.appbooster.presentation.screen.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.schedapp.presentation.screen.common.basescreen.ErrorDialogConfig
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.common.OptimizationResult
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.presentation.permission.NotificationPermissionManager
import com.tony.appbooster.presentation.permission.NotificationPermissionRationaleDialog
import com.tony.appbooster.presentation.screen.common.basescreen.AppBaseScreen
import com.tony.appbooster.presentation.screen.dashboard.components.OptimizationActivityFeed
import com.tony.appbooster.presentation.viewmodel.main.MainUiEffect
import com.tony.appbooster.presentation.viewmodel.main.MainUiEvent
import com.tony.appbooster.presentation.viewmodel.main.MainUiModel
import com.tony.appbooster.presentation.viewmodel.main.MainViewModel
import kotlinx.coroutines.flow.collectLatest

private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var pendingOptimizationStart by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        if (pendingOptimizationStart) {
            pendingOptimizationStart = false
            viewModel.onEvent(MainUiEvent.OnStartOptimizationClicked)
        }
    }

    if (showNotificationPermissionDialog) {
        NotificationPermissionRationaleDialog(
            onConfirm = {
                showNotificationPermissionDialog = false
                pendingOptimizationStart = true
                notificationPermissionLauncher.launch(POST_NOTIFICATIONS_PERMISSION)
            },
            onDismiss = {
                showNotificationPermissionDialog = false
                viewModel.onEvent(MainUiEvent.OnStartOptimizationClicked)
            }
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is MainUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AppBaseScreen(
        uiState = uiState,
        errorDialogConfig = ErrorDialogConfig(
            onCancel = { viewModel.showErrorPopup(false) }
        )
    ) { model ->
        DashboardContent(
            model = model,
            onStartOptimization = {
                if (NotificationPermissionManager.shouldRequest(context)) {
                    showNotificationPermissionDialog = true
                } else {
                    viewModel.onEvent(MainUiEvent.OnStartOptimizationClicked)
                }
            },
            onStopOptimization = { viewModel.onEvent(MainUiEvent.OnStopOptimizationClicked) },
            onDismissResult = { viewModel.onEvent(MainUiEvent.OnDismissOptimizationResultClicked) },
            onAnalyze = { viewModel.onEvent(MainUiEvent.OnAnalyzeAppsClicked) },
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    model: MainUiModel,
    onStartOptimization: () -> Unit,
    onStopOptimization: () -> Unit,
    onDismissResult: () -> Unit,
    onAnalyze: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())


    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    // Intentionally empty: connection chip removed (Shizuku health is validated on-demand).
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card - always visible, changes content based on state
            HeroControlPanel(model, onStartOptimization, onStopOptimization, onDismissResult, onAnalyze)

            // Activity feed - always visible
            OptimizationActivityFeed(
                entries = model.logEntries,
                isExpanded = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
            )
        }
    }
}


/**
 * Hero control panel with gradient background and expressive animations.
 * Provides the main optimization start/progress interface.
 */
@Composable
private fun HeroControlPanel(
    model: MainUiModel,
    onStartOptimization: () -> Unit,
    onStopOptimization: () -> Unit,
    onDismissResult: () -> Unit,
    onAnalyze: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Animated gradient rotation
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (model.optimizationProgress.isRunning) 8.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cardElevation"
    )

    // Result banners are dismissible per run; store dismissal in ViewModel-backed state.
    val isResultDismissed = model.dismissedResultRunIds.contains(model.optimizationProgress.runId)

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Subtle animated gradient overlay
                    val startX = size.width * gradientOffset
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.05f),
                                tertiaryColor.copy(alpha = 0.08f),
                                primaryColor.copy(alpha = 0.03f)
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + size.width, size.height)
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = model.optimizationProgress.result,
                    label = "OptimizationStateAnimation",
                    transitionSpec = {
                        (slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = tween(400, easing = EaseOutBack)
                        ) + fadeIn(tween(300)))
                            .togetherWith(
                                slideOutVertically(
                                    targetOffsetY = { -it / 3 },
                                    animationSpec = tween(300, easing = EaseOutCubic)
                                ) + fadeOut(tween(200))
                            )
                            .using(SizeTransform(clip = false))
                    }
                ) { result ->
                    when {
                        // Special case: All apps already optimized (no work was needed)
                        result is OptimizationResult.Completed &&
                                model.optimizationProgress.processedCount == 0 &&
                                model.optimizationProgress.skippedCount > 0 &&
                                !isResultDismissed -> {
                            AllOptimizedContent(
                                skippedCount = model.optimizationProgress.skippedCount,
                                onDismiss = onDismissResult
                            )
                        }

                        result is OptimizationResult.Completed && !isResultDismissed -> {
                            OptimizationCompletedContent(
                                processedCount = model.optimizationProgress.processedCount,
                                skippedCount = model.optimizationProgress.skippedCount,
                                totalCount = model.optimizationProgress.totalCount,
                                onDismiss = onDismissResult,
                                onRunAgain = onStartOptimization
                            )
                        }

                        result is OptimizationResult.Canceled && !isResultDismissed -> {
                            OptimizationCanceledContent(
                                processedCount = model.optimizationProgress.processedCount,
                                skippedCount = model.optimizationProgress.skippedCount,
                                totalCount = model.optimizationProgress.totalCount,
                                onDismiss = onDismissResult,
                                onRunAgain = onStartOptimization
                            )
                        }

                        // Optimization is running - show progress bar and stop button
                        model.optimizationProgress.isRunning -> {
                            OptimizationRunningContent(
                                progress = model.optimizationProgress.progress,
                                processedCount = model.optimizationProgress.processedCount,
                                totalCount = model.optimizationProgress.totalCount,
                                currentAppPackage = model.optimizationProgress.currentAppPackage,
                                onStop = onStopOptimization
                            )
                        }

                        else -> {
                            OptimizationReadyContent(
                                optimizationMode = model.optimizationMode,
                                analysis = model.optimizationAnalysis,
                                isStarting = model.isStartingOptimization,
                                onStartOptimization = onStartOptimization,
                                onAnalyze = onAnalyze
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Content displayed while optimization is running with horizontal progress bar.
 */
@Composable
private fun OptimizationRunningContent(
    progress: Float,
    processedCount: Int,
    totalCount: Int,
    currentAppPackage: String,
    onStop: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "progressAnimation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress info row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dashboard_optimizing_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$processedCount / $totalCount apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Percentage badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Horizontal progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }

        // Stop button - full width
        Surface(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.StopCircle,
                    contentDescription = stringResource(R.string.dashboard_stop_optimization_cd),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.action_stop),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}


@Composable
private fun OptimizationReadyContent(
    optimizationMode: AppOptimizationType,
    analysis: com.tony.appbooster.domain.model.common.OptimizationAnalysis,
    isStarting: Boolean = false,
    onStartOptimization: () -> Unit,
    onAnalyze: () -> Unit
) {
    // Show minimal scanning UI when analyzing
    if (analysis.isScanning) {
        ScanningContent()
        return
    }

    // Subtle floating animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "idle")
    val iconOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconOffset"
    )


    // Dynamic content based on optimization mode
    val (title, description, icon) = when (optimizationMode) {
        AppOptimizationType.SPEED_PROFILE -> Triple(
            stringResource(R.string.dashboard_ready_speed_profile_title),
            stringResource(R.string.dashboard_ready_speed_profile_description),
            Icons.Rounded.Speed
        )
        AppOptimizationType.FULL_OPTIMIZATION -> Triple(
            stringResource(R.string.dashboard_ready_full_optimization_title),
            stringResource(R.string.dashboard_ready_full_optimization_description),
            Icons.Rounded.RocketLaunch
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated icon
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { translationY = iconOffset },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                FilledTonalButton(
                    onClick = onStartOptimization,
                    enabled = !isStarting,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (isStarting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Starting...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_start),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Pre-scan analysis section
        when {
            analysis.hasScanned -> {
                // Show analysis results
                OptimizationStatsRow(
                    optimizedCount = analysis.appsNeedingOptimization,
                    skippedCount = analysis.appsAlreadyOptimized
                )
            }
            else -> {
                // Not scanned yet - show scan button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onAnalyze),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.analysis_scanning_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.analysis_tap_to_scan),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimal and beautiful scanning state content.
 */
@Composable
private fun ScanningContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated search icon
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha),
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.analysis_scanning_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.analysis_scanning_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha)
            )
        }
    }
}

/**
 * Expressive completion panel displayed after a successful optimization run.
 *
 * @param processedCount Number of optimized apps.
 * @param skippedCount Number of apps skipped (already optimized recently).
 * @param totalCount Total apps targeted by the run.
 * @param onDismiss Callback to hide this panel until the next run.
 * @param onRunAgain Callback to start optimization again.
 */
@Composable
private fun OptimizationCompletedContent(
    processedCount: Int,
    skippedCount: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    onRunAgain: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "completed")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(72.dp)
                .scale(iconScale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.dashboard_result_completed_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(
                R.string.dashboard_result_completed_count,
                processedCount,
                totalCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Smart optimization stats card
        if (skippedCount > 0) {
            Spacer(Modifier.height(16.dp))
            OptimizationStatsRow(
                optimizedCount = processedCount,
                skippedCount = skippedCount
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onRunAgain,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_run_again))
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
    }
}

/**
 * Expressive result panel displayed when an optimization run is cancelled.
 *
 * Business purpose:
 * - Distinguishes cancellation (including WorkManager/notification stop) from success.
 * - Provides a clear next action (run again) without implying the run succeeded.
 *
 * @param processedCount Number of apps optimized before cancellation.
 * @param skippedCount Number of apps skipped (already optimized recently).
 * @param totalCount Total apps targeted by the run.
 * @param onDismiss Callback to hide this panel until the next run.
 * @param onRunAgain Callback to start optimization again.
 */
@Composable
private fun OptimizationCanceledContent(
    processedCount: Int,
    skippedCount: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    onRunAgain: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "canceled")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(72.dp)
                .scale(iconScale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.DoNotDisturbOn,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.dashboard_result_canceled_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(
                R.string.dashboard_result_canceled_count,
                processedCount,
                totalCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Smart optimization stats card
        if (skippedCount > 0 || processedCount > 0) {
            Spacer(Modifier.height(16.dp))
            OptimizationStatsRow(
                optimizedCount = processedCount,
                skippedCount = skippedCount
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onRunAgain,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_run_again))
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
    }
}

/**
 * Celebratory content displayed when all apps are already optimized.
 *
 * Business purpose:
 * - Provides positive feedback when the device is already at peak performance.
 * - Shows the count of apps that were analyzed and found to be optimal.
 *
 * @param skippedCount Number of apps that are already optimized.
 * @param onDismiss Callback to hide this panel.
 */
@Composable
private fun AllOptimizedContent(
    skippedCount: Int,
    onDismiss: () -> Unit
) {
    // Celebratory animation
    val infiniteTransition = rememberInfiniteTransition(label = "allOptimized")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    // Subtle glow effect rotation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Celebratory icon with glow effect
        Box(contentAlignment = Alignment.Center) {
            // Glow ring
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .scale(iconScale)
                    .alpha(glowAlpha),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {}

            // Main icon
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .scale(iconScale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.analysis_all_optimized_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = stringResource(R.string.analysis_all_optimized_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Stats badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = stringResource(R.string.analysis_apps_already_optimized, skippedCount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_dismiss))
        }
    }
}


/**
 * Beautiful stats row showing optimization vs skipped apps with animated counters
 * and expressive visual indicators.
 *
 * @param optimizedCount Number of apps that were optimized.
 * @param skippedCount Number of apps skipped because they were already optimized.
 */
@Composable
private fun OptimizationStatsRow(
    optimizedCount: Int,
    skippedCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Optimized apps stat card
        StatCard(
            modifier = Modifier.weight(1f),
            count = optimizedCount,
            label = stringResource(R.string.analysis_card_needs_optimization),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Rounded.RocketLaunch
        )

        // Already optimized apps stat card
        StatCard(
            modifier = Modifier.weight(1f),
            count = skippedCount,
            label = stringResource(R.string.analysis_card_optimized),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.Rounded.CheckCircle
        )
    }
}

/**
 * Individual stat card with animated count display and icon.
 *
 * @param modifier Modifier for layout customization.
 * @param count The numerical value to display.
 * @param label Descriptive label for the stat.
 * @param containerColor Background color of the card.
 * @param contentColor Color for text and icons.
 * @param icon Icon to display alongside the count.
 */
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    // Animate the count value for a polished entrance effect
    val animatedCount by animateFloatAsState(
        targetValue = count.toFloat(),
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "countAnimation"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Text(
                    text = animatedCount.toInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

