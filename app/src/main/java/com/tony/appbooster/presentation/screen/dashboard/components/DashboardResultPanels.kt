package com.tony.appbooster.presentation.screen.dashboard.components

import android.content.res.Configuration
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tony.appbooster.R
import com.tony.appbooster.presentation.ui.theme.AppBoosterTheme


// ─────────────────────────────────────────────────────────────────────────────
// Unified result panel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Generalized hero result panel that renders the post-run outcome of an
 * optimization session.
 *
 * Replaces the three previous per-outcome composables with a single implementation
 * driven by [HeroCardStatus].
 *
 * @param status Sealed outcome that describes which result variant to render.
 * @param modifier Optional layout modifier.
 * @param onDismiss Callback invoked when the user dismisses the result panel.
 * @param onRunAgain Callback invoked when the user requests another optimization
 *   run. Unused for [HeroCardStatus.AllOptimized].
 */
@Composable
fun HeroResultPanel(
    status: HeroCardStatus,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onRunAgain: () -> Unit = {},
) {
    // Derive visual tokens from the status variant
    val config = rememberHeroResultConfig(status)

    val infiniteTransition = rememberInfiniteTransition(label = "heroResultPanel")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = config.iconScaleTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(config.pulseMs, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )
    // Extra glow pulse used only for AllOptimized
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
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Icon badge ─────────────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            // Glow halo – only visible for AllOptimized
            if (config.showGlow) {
                Surface(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(iconScale)
                        .alpha(glowAlpha),
                    shape = CircleShape,
                    color = config.glowColor
                ) {}
            }

            Surface(
                modifier = Modifier
                    .size(if (config.showGlow) 72.dp else 72.dp)
                    .scale(iconScale),
                shape = CircleShape,
                color = config.containerColor,
                tonalElevation = if (config.showGlow) 6.dp else 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (config.showGlow) 42.dp else 38.dp),
                        tint = config.iconTint
                    )
                }
            }
        }

        Spacer(Modifier.height(if (config.showGlow) 20.dp else 16.dp))

        // ── Title ──────────────────────────────────────────────────────────
        Text(
            text = config.title,
            style = if (config.showGlow) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // ── Subtitle / count ───────────────────────────────────────────────
        Text(
            text = config.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = if (config.showGlow) 16.dp else 0.dp)
        )

        // ── Tertiary chip – AllOptimized only ──────────────────────────────
        if (config.showGlow) {
            val allOptimized = status as HeroCardStatus.AllOptimized
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
                        text = stringResource(
                            R.string.analysis_apps_already_optimized,
                            allOptimized.optimizedCount
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // ── Stats row (Completed and Canceled only) ───────────────────────
        val showStats = when (status) {
            is HeroCardStatus.Completed    -> status.processedCount > 0 || status.skippedCount > 0
            is HeroCardStatus.Canceled     -> status.processedCount > 0 || status.skippedCount > 0
            is HeroCardStatus.AllOptimized -> false
        }

        if (showStats) {
            Spacer(Modifier.height(16.dp))
            if (status is HeroCardStatus.Completed) {
                OptimizationStatsRow(
                    needsOptimizationCount = 0,
                    // processedCount = freshly compiled; skippedCount = already optimal
                    optimizedCount = status.processedCount,
                    noProfileCount = status.skippedCount
                )
            } else if (status is HeroCardStatus.Canceled) {
                OptimizationStatsRow(
                    needsOptimizationCount = (status.totalCount - (status.processedCount + status.skippedCount))
                        .coerceAtLeast(0),
                    optimizedCount = status.processedCount + status.skippedCount
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Action buttons ─────────────────────────────────────────────────
        if (config.showRunAgain) {
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
        } else {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal config helper
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Immutable visual configuration resolved from a [HeroCardStatus] variant.
 * Keeps the rendering composable free from branching on status type.
 *
 * @property icon Vector icon rendered inside the badge surface.
 * @property containerColor Background color of the circular icon badge.
 * @property iconTint Tint applied to [icon].
 * @property glowColor Color of the decorative outer glow ring (only used when [showGlow] is true).
 * @property title Primary headline string.
 * @property subtitle Secondary descriptive string.
 * @property iconScaleTarget Upper bound of the looping scale pulse.
 * @property pulseMs Duration of one half of the scale pulse animation in milliseconds.
 * @property showGlow Whether to render the decorative outer glow ring (AllOptimized only).
 * @property showRunAgain Whether to show the "Run again" CTA button.
 */
private data class HeroResultConfig(
    val icon: ImageVector,
    val containerColor: Color,
    val iconTint: Color,
    val glowColor: Color,
    val title: String,
    val subtitle: String,
    val iconScaleTarget: Float,
    val pulseMs: Int,
    val showGlow: Boolean,
    val showRunAgain: Boolean
)

@Composable
private fun rememberHeroResultConfig(status: HeroCardStatus): HeroResultConfig {
    val colorScheme = MaterialTheme.colorScheme
    return when (status) {
        is HeroCardStatus.Completed -> HeroResultConfig(
            icon = Icons.Rounded.CheckCircle,
            containerColor = colorScheme.primaryContainer,
            iconTint = colorScheme.onPrimaryContainer,
            glowColor = Color.Transparent,
            title = stringResource(R.string.dashboard_result_completed_title),
            subtitle = stringResource(
                R.string.dashboard_result_completed_count,
                status.processedCount,
                // denominator = apps actually dealt with, not the original target total
                status.processedCount + status.skippedCount
            ),
            iconScaleTarget = 1.06f,
            pulseMs = 800,
            showGlow = false,
            showRunAgain = true
        )
        is HeroCardStatus.Canceled -> HeroResultConfig(
            icon = Icons.Rounded.DoNotDisturbOn,
            containerColor = colorScheme.errorContainer,
            iconTint = colorScheme.onErrorContainer,
            glowColor = Color.Transparent,
            title = stringResource(R.string.dashboard_result_canceled_title),
            subtitle = stringResource(
                R.string.dashboard_result_canceled_count,
                status.processedCount,
                status.totalCount
            ),
            iconScaleTarget = 1.04f,
            pulseMs = 900,
            showGlow = false,
            showRunAgain = true
        )
        is HeroCardStatus.AllOptimized -> HeroResultConfig(
            icon = Icons.Rounded.CheckCircle,
            containerColor = colorScheme.primaryContainer,
            iconTint = colorScheme.primary,
            glowColor = colorScheme.primary.copy(alpha = 0.2f),
            title = stringResource(R.string.analysis_all_optimized_title),
            subtitle = stringResource(R.string.analysis_all_optimized_subtitle),
            iconScaleTarget = 1.08f,
            pulseMs = 1000,
            showGlow = true,
            showRunAgain = false
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(name = "Completed – Light", showBackground = true)
@Preview(name = "Completed – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HeroResultPanelCompletedPreview() {
    AppBoosterTheme {
        HeroResultPanel(
            status = HeroCardStatus.Completed(processedCount = 18, skippedCount = 4, totalCount = 22),
            onDismiss = {},
            onRunAgain = {}
        )
    }
}

@Preview(name = "Canceled – Light", showBackground = true)
@Preview(name = "Canceled – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HeroResultPanelCanceledPreview() {
    AppBoosterTheme {
        HeroResultPanel(
            status = HeroCardStatus.Canceled(processedCount = 7, skippedCount = 2, totalCount = 22),
            onDismiss = {},
            onRunAgain = {}
        )
    }
}

@Preview(name = "All Optimized – Light", showBackground = true)
@Preview(name = "All Optimized – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HeroResultPanelAllOptimizedPreview() {
    AppBoosterTheme {
        HeroResultPanel(
            status = HeroCardStatus.AllOptimized(optimizedCount = 22, noProfileCount = 3),
            onDismiss = {}
        )
    }
}
