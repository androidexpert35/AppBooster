package com.tony.appbooster.presentation.screen.dashboard.components

import androidx.compose.animation.core.EaseInOutCubic
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tony.appbooster.R

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
fun OptimizationCompletedContent(
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
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
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
            modifier = Modifier.height(0.dp) // keep layout stable when animating
        )

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
 * @param processedCount Number of apps optimized before cancellation.
 * @param skippedCount Number of apps skipped (already optimized recently).
 * @param totalCount Total apps targeted by the run.
 * @param onDismiss Callback to hide this panel until the next run.
 * @param onRunAgain Callback to start optimization again.
 */
@Composable
fun OptimizationCanceledContent(
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
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
 * @param skippedCount Number of apps that are already optimized.
 * @param onDismiss Callback to hide this panel.
 */
@Composable
fun AllOptimizedContent(
    skippedCount: Int,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "allOptimized")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "iconScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .scale(iconScale)
                    .alpha(glowAlpha),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {}

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
