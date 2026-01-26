package com.tony.appbooster.presentation.screen.dashboard.components

import android.content.res.Configuration
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tony.appbooster.R
import com.tony.appbooster.presentation.tools.AppBasePreview
import com.tony.appbooster.presentation.ui.theme.AppBoosterTheme
import com.tony.appbooster.presentation.ui.theme.paddingMedium

/**
 * Beautiful stats row showing optimization vs skipped apps.
 *
 * @param needsOptimizationCount Number of apps that still need optimization.
 * @param optimizedCount Number of apps already optimized.
 */
@Composable
fun OptimizationStatsRow(
    needsOptimizationCount: Int,
    optimizedCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            count = needsOptimizationCount,
            label = stringResource(R.string.analysis_card_needs_optimization),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Rounded.RocketLaunch
        )

        StatCard(
            modifier = Modifier.weight(1f),
            count = optimizedCount,
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
fun StatCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector
) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = paddingMedium),
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
                    style = MaterialTheme.typography.headlineSmall,
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

@AppBasePreview
@Composable
private fun OptimizationStatsRowPreview() {
    AppBoosterTheme {
        OptimizationStatsRow(
            needsOptimizationCount = 42,
            optimizedCount = 180
        )
    }
}

@Preview(name = "Large Font", fontScale = 1.5f, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun OptimizationStatsRowLargeFontPreview() {
    AppBoosterTheme {
        OptimizationStatsRow(
            needsOptimizationCount = 120,
            optimizedCount = 12
        )
    }
}

@AppBasePreview
@Composable
private fun StatCardPreview() {
    AppBoosterTheme {
        StatCard(
            count = 99,
            label = "Needs optimization",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Rounded.RocketLaunch
        )
    }
}
