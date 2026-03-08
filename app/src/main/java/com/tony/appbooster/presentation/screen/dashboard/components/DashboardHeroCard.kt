package com.tony.appbooster.presentation.screen.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.common.OptimizationResult
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.presentation.viewmodel.main.MainUiModel

/**
 * Renders the dashboard hero card that drives the core user journey:
 * - Start optimization
 * - Analyze optimization state
 * - Show running progress and stop controls
 * - Show completion/cancellation summaries
 *
 * Business purpose:
 * - Provides a focused “control center” that keeps the main actions visible
 *   while the activity feed communicates detailed progress.
 *
 * @param model Current dashboard state.
 * @param onStartOptimization User intent: start optimization.
 * @param onForceOptimize User intent: force re-optimize all apps regardless of status.
 * @param onStopOptimization User intent: cancel optimization.
 * @param onDismissResult User intent: dismiss the result banner.
 * @param onAnalyze User intent: start analysis.
 * @param onStopAnalysis User intent: cancel analysis.
 */
@Composable
fun DashboardHeroCard(
    model: MainUiModel,
    onStartOptimization: () -> Unit,
    onForceOptimize: () -> Unit,
    onStopOptimization: () -> Unit,
    onDismissResult: () -> Unit,
    onAnalyze: () -> Unit,
    onStopAnalysis: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Animated gradient offset (subtle, avoids heavy visuals)
    val infiniteTransition = rememberInfiniteTransition(label = "dashboardHeroGradient")
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
        targetValue = if (model.optimizationProgress.isRunning || model.optimizationAnalysis.isScanning) 8.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cardElevation"
    )

    // Derive a stable phase key so AnimatedContent only transitions when the
    // *phase* changes (Idle → Optimizing → Completed, etc.).
    // Without this, keying on the full OptimizationProgress object causes the
    // bounce animation to fire on every per-app progress tick.
    val heroPhase by remember(model) {
        derivedStateOf {
            val progress = model.optimizationProgress
            val result = progress.result
            when {
                result is OptimizationResult.Completed
                    && progress.processedCount == 0
                    && progress.skippedCount > 0
                    && !model.isCurrentResultDismissed -> HeroPhase.ALL_OPTIMIZED

                result is OptimizationResult.Completed
                    && !model.isCurrentResultDismissed -> HeroPhase.RESULT_COMPLETED

                result is OptimizationResult.Canceled
                    && !model.isCurrentResultDismissed -> HeroPhase.RESULT_CANCELED

                progress.isRunning -> HeroPhase.OPTIMIZING

                model.optimizationAnalysis.isScanning -> HeroPhase.SCANNING

                else -> HeroPhase.READY
            }
        }
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = heroPhase,
                label = "heroResultSwap",
                transitionSpec = {
                    (slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(400, easing = EaseOutBack)
                    ) + fadeIn(tween(300))).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { -it / 3 },
                            animationSpec = tween(300, easing = EaseOutCubic)
                        ) + fadeOut(tween(200))
                    ).using(SizeTransform(clip = false))
                }
            ) { phase ->
                // Live data is read from `model` (outer scope) so progress ticks
                // cause normal recomposition without triggering a new animation.
                when (phase) {
                    HeroPhase.ALL_OPTIMIZED -> {
                        HeroResultPanel(
                            status = HeroCardStatus.AllOptimized(
                                optimizedCount = model.optimizationAnalysis.appsAlreadyOptimized,
                                noProfileCount = model.optimizationAnalysis.appsWithNoProfile,
                                optimizationMode = model.optimizationMode
                            ),
                            onDismiss = onDismissResult,
                            onForceOptimize = onForceOptimize
                        )
                    }

                    HeroPhase.RESULT_COMPLETED -> {
                        HeroResultPanel(
                            status = HeroCardStatus.Completed(
                                processedCount = model.optimizationProgress.processedCount,
                                skippedCount = model.optimizationProgress.skippedCount,
                                totalCount = model.optimizationProgress.totalCount,
                                noProfileCount = model.optimizationAnalysis.appsWithNoProfile,
                                optimizationMode = model.optimizationMode
                            ),
                            onDismiss = onDismissResult,
                            onRunAgain = onStartOptimization,
                            onForceOptimize = onForceOptimize
                        )
                    }

                    HeroPhase.RESULT_CANCELED -> {
                        HeroResultPanel(
                            status = HeroCardStatus.Canceled(
                                processedCount = model.optimizationProgress.processedCount,
                                skippedCount = model.optimizationProgress.skippedCount,
                                totalCount = model.optimizationProgress.totalCount,
                                noProfileCount = model.optimizationAnalysis.appsWithNoProfile,
                                optimizationMode = model.optimizationMode
                            ),
                            onDismiss = onDismissResult,
                            onRunAgain = onStartOptimization,
                            onForceOptimize = onForceOptimize
                        )
                    }

                    HeroPhase.OPTIMIZING -> {
                        ProcessProgressContent(
                            state = ProcessProgressState.fromOptimizationProgress(
                                progress = model.optimizationProgress,
                                titleText = stringResource(R.string.dashboard_optimizing_title)
                            ),
                            onStop = onStopOptimization
                        )
                    }

                    HeroPhase.SCANNING -> {
                        ProcessProgressContent(
                            state = ProcessProgressState.fromOptimizationAnalysis(
                                analysis = model.optimizationAnalysis,
                                titleText = stringResource(R.string.analysis_scanning_title),
                                subtitleText = stringResource(R.string.analysis_scanning_subtitle)
                            ),
                            onStop = onStopAnalysis
                        )
                    }

                    HeroPhase.READY -> {
                        ReadyContent(
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

@Composable
private fun ReadyContent(
    optimizationMode: AppOptimizationType,
    analysis: com.tony.appbooster.domain.model.common.OptimizationAnalysis,
    isStarting: Boolean,
    onStartOptimization: () -> Unit,
    onAnalyze: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroIdle")
    val iconOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconOffset"
    )

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
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { translationY = iconOffset },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
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
                        text = stringResource(R.string.action_start),
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

        when {
            analysis.hasScanned -> {
                    OptimizationStatsRow(
                        needsOptimizationCount = analysis.appsNeedingOptimization,
                        optimizedCount = analysis.appsAlreadyOptimized,
                        noProfileCount = analysis.appsWithNoProfile,
                        showNoProfile = optimizationMode == AppOptimizationType.SPEED_PROFILE
                    )
                }

            else -> {
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
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
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
 * Stable discriminator used as the [AnimatedContent] key in [DashboardHeroCard].
 *
 * Keying on this enum instead of the full domain progress object ensures that
 * the slide/fade transition fires only when the *phase* changes (e.g. Idle → Optimizing),
 * not on every per-app progress tick within the same phase.
 */
private enum class HeroPhase {
    READY,
    SCANNING,
    OPTIMIZING,
    RESULT_COMPLETED,
    RESULT_CANCELED,
    ALL_OPTIMIZED
}
