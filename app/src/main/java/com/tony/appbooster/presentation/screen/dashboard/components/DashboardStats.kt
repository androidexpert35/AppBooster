package com.tony.appbooster.presentation.screen.dashboard.components

import android.content.res.Configuration
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tony.appbooster.R
import com.tony.appbooster.presentation.tools.AppBasePreview
import com.tony.appbooster.presentation.ui.theme.AppBoosterTheme

/**
 * Compact vertical stats summary showing optimization outcome counters.
 *
 * Renders each stat as an icon-leading row inside a single tonal surface so
 * label length never affects layout — long translated strings wrap naturally
 * without breaking the alignment of sibling rows.
 *
 * @param needsOptimizationCount Number of apps that still need optimization.
 * @param optimizedCount Number of apps already optimized.
 * @param noProfileCount Number of apps with no runtime profile; row is omitted when 0.
 */
@Composable
fun OptimizationStatsRow(
    needsOptimizationCount: Int,
    optimizedCount: Int,
    noProfileCount: Int = 0
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (needsOptimizationCount > 0) {
                StatRow(
                    count = needsOptimizationCount,
                    label = stringResource(R.string.analysis_card_needs_optimization),
                    dotColor = MaterialTheme.colorScheme.primary
                )
            }
            StatRow(
                count = optimizedCount,
                label = stringResource(R.string.analysis_card_optimized),
                dotColor = MaterialTheme.colorScheme.tertiary
            )
            if (noProfileCount > 0) {
                StatRow(
                    count = noProfileCount,
                    label = stringResource(R.string.analysis_card_no_profile),
                    dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}


/**
 * Single icon-leading stat row: coloured dot • bold count • flexible label.
 *
 * @param count Numerical value to display.
 * @param label Descriptive label; wraps freely without breaking row alignment.
 * @param dotColor Accent colour used for the leading dot indicator.
 * @param modifier Optional layout modifier.
 */
@Composable
private fun StatRow(
    count: Int,
    label: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedCount by animateFloatAsState(
        targetValue = count.toFloat(),
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "statRowCount"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Coloured dot indicator
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = dotColor
        ) {}

        // Count – fixed intrinsic width, never shrinks
        Text(
            text = animatedCount.toInt().toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp)
        )

        // Label – takes remaining space, wraps safely
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // Dot accent repeated on trailing edge for visual balance
        Box(modifier = Modifier.size(6.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@AppBasePreview
@Composable
private fun OptimizationStatsRowPreview() {
    AppBoosterTheme {
        OptimizationStatsRow(
            needsOptimizationCount = 42,
            optimizedCount = 180,
            noProfileCount = 23
        )
    }
}

@Preview(name = "Large Font", fontScale = 1.5f, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun OptimizationStatsRowLargeFontPreview() {
    AppBoosterTheme {
        OptimizationStatsRow(
            needsOptimizationCount = 120,
            optimizedCount = 12,
            noProfileCount = 0
        )
    }
}

@Preview(name = "No pending", showBackground = true)
@Composable
private fun OptimizationStatsRowNoPendingPreview() {
    AppBoosterTheme {
        OptimizationStatsRow(
            needsOptimizationCount = 0,
            optimizedCount = 47,
            noProfileCount = 5
        )
    }
}
