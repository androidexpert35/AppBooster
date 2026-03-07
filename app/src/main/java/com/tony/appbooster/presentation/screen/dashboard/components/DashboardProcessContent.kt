package com.tony.appbooster.presentation.screen.dashboard.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.common.OptimizationAnalysis

/**
 * Unified progress content for both analysis and optimization processes.
 * Provides a consistent UI with title, progress bar, current app, stats, and stop button.
 *
 * @param title The title to display (e.g., "Analyzing Apps" or "Optimizing Apps").
 * @param subtitle The subtitle showing progress (e.g., "10 / 50 apps").
 * @param progress Progress value from 0f to 1f.
 * @param currentPackage Package currently being processed, empty if none.
 * @param statChips List of [ProcessStatChip] items to display in the stats row.
 *   Pass an empty list to hide the stats row entirely.
 * @param onStop Callback when stop button is pressed.
 */
@Composable
internal fun ProcessProgressContent(
    title: String,
    subtitle: String,
    progress: Float,
    currentPackage: String,
    statChips: List<ProcessStatChip> = emptyList(),
    onStop: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "progressAnimation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header row with title and percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
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

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
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

        // Current app being processed
        if (currentPackage.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = currentPackage.substringAfterLast(".").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentPackage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Stats row – rendered only when at least one chip is provided
        if (statChips.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statChips.forEach { chip ->
                    StatChipItem(
                        chip = chip,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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

/**
 * Renders a single stat chip inside the progress stats row.
 *
 * Visual colour tokens are resolved from [ProcessStatChip.style] so no
 * branching is needed at the call site.
 *
 * @param chip Data model describing the chip's count, label, icon, and style.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun StatChipItem(
    chip: ProcessStatChip,
    modifier: Modifier = Modifier
) {
    // Resolve M3 colour tokens from the style variant
    val containerColor: Color
    val contentColor: Color
    val labelColor: Color

    when (chip.style) {
        ProcessStatChipStyle.Pending -> {
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            contentColor = MaterialTheme.colorScheme.error
            labelColor = MaterialTheme.colorScheme.onErrorContainer
        }
        ProcessStatChipStyle.Neutral -> {
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        ProcessStatChipStyle.Done -> {
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            contentColor = MaterialTheme.colorScheme.primary
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = chip.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${chip.count}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = chip.label,
                style = MaterialTheme.typography.bodySmall,
                color = labelColor
            )
        }
    }
}

/**
 * Analysis scanning content that delegates to [ProcessProgressContent].
 *
 * Constructs [ProcessStatChip] instances from the current [OptimizationAnalysis]
 * state and passes them as a typed list, avoiding raw [Pair] usage.
 *
 * @param analysis Current analysis state with progress info.
 * @param onStop Callback when stop button is pressed.
 */
@Composable
internal fun ScanningContent(
    analysis: OptimizationAnalysis,
    onStop: () -> Unit
) {
    // Build chips only once scan has produced data worth showing
    val chips = if (analysis.totalAppsScanned > 0) {
        buildList {
            add(
                ProcessStatChip(
                    count = analysis.appsNeedingOptimization,
                    label = "need",
                    icon = Icons.Rounded.RocketLaunch,
                    style = ProcessStatChipStyle.Pending
                )
            )
            // No-profile chip is shown only when there are matching apps
            if (analysis.appsWithNoProfile > 0) {
                add(
                    ProcessStatChip(
                        count = analysis.appsWithNoProfile,
                        label = "no profile",
                        icon = Icons.Rounded.PersonOff,
                        style = ProcessStatChipStyle.Neutral
                    )
                )
            }
            add(
                ProcessStatChip(
                    count = analysis.appsAlreadyOptimized,
                    label = "done",
                    icon = Icons.Rounded.CheckCircle,
                    style = ProcessStatChipStyle.Done
                )
            )
        }
    } else {
        emptyList()
    }

    ProcessProgressContent(
        title = stringResource(R.string.analysis_scanning_title),
        subtitle = if (analysis.totalAppsToScan > 0)
            "${analysis.totalAppsScanned} / ${analysis.totalAppsToScan} apps"
        else
            stringResource(R.string.analysis_scanning_subtitle),
        progress = analysis.progress,
        currentPackage = analysis.currentPackage,
        statChips = chips,
        onStop = onStop
    )
}
