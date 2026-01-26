package com.tony.appbooster.presentation.screen.dashboard.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tony.appbooster.domain.model.common.LogEntryType
import com.tony.appbooster.domain.model.common.OptimizationLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper data class for app display information.
 *
 * @property icon The app's icon drawable.
 * @property label The app's display label.
 */
private data class AppDisplayInfo(
    val icon: Drawable,
    val label: String
)

/**
 * Beautiful circular progress indicator with smooth animation.
 *
 * @param progress Current progress from 0f to 1f.
 * @param processedCount Number of apps processed so far.
 * @param totalCount Total number of apps to process.
 * @param currentApp Package name of current app being processed.
 * @param modifier Modifier for layout customization.
 */
@Composable
fun CircularOptimizationProgress(
    progress: Float,
    processedCount: Int,
    totalCount: Int,
    currentApp: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "progressAnimation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background track
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            drawCircle(
                color = surfaceVariant,
                radius = (size.minDimension - strokeWidth) / 2,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Progress arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val sweepAngle = animatedProgress * 360f

            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Percentage
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Count
            Text(
                text = "$processedCount / $totalCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Current app (truncated)
            if (currentApp.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentApp.substringAfterLast("."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/**
 * Stats summary bar showing optimization statistics.
 */
@Composable
fun OptimizationStatsBar(
    optimizedCount: Int,
    skippedCount: Int,
    failedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(
            count = optimizedCount,
            label = "Optimized",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Rounded.CheckCircle,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            count = skippedCount,
            label = "Skipped",
            color = MaterialTheme.colorScheme.tertiary,
            icon = Icons.Rounded.FastForward,
            modifier = Modifier.weight(1f)
        )
        if (failedCount > 0) {
            StatChip(
                count = failedCount,
                label = "Failed",
                color = MaterialTheme.colorScheme.error,
                icon = Icons.Rounded.Error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual stat chip with icon and count.
 */
@Composable
private fun StatChip(
    count: Int,
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val animatedCount by animateFloatAsState(
        targetValue = count.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "countAnimation"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = animatedCount.toInt().toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Beautiful activity feed showing recent optimization events.
 *
 * @param entries List of log entries to display.
 * @param isExpanded Whether the feed should fill available space.
 * @param modifier Modifier for layout customization.
 */
@Composable
fun OptimizationActivityFeed(
    entries: List<OptimizationLogEntry>,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to latest entry
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .then(if (isExpanded) Modifier.fillMaxSize() else Modifier)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            if (entries.isEmpty()) {
                // Empty state - fills available space when expanded
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isExpanded) Modifier.weight(1f) else Modifier.height(120.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .alpha(0.4f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Ready to optimize",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap Start to begin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = if (isExpanded) Modifier.weight(1f) else Modifier.height(200.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true // Show newest at top
                ) {
                    items(
                        items = entries.takeLast(50), // Keep last 50 entries
                        key = { it.id }
                    ) { entry ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            ) + fadeIn() + scaleIn(initialScale = 0.9f)
                        ) {
                            ActivityLogItem(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single activity log item with expressive styling based on type.
 */
@Composable
private fun ActivityLogItem(
    entry: OptimizationLogEntry
) {
    val (icon, color, bgColor) = remember(entry.type) {
        when (entry.type) {
            LogEntryType.SUCCESS -> Triple(
                Icons.Rounded.CheckCircle,
                Color(0xFF4CAF50),
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
            LogEntryType.SKIPPED -> Triple(
                Icons.Rounded.FastForward,
                Color(0xFFFF9800),
                Color(0xFFFF9800).copy(alpha = 0.1f)
            )
            LogEntryType.ERROR -> Triple(
                Icons.Rounded.Error,
                Color(0xFFF44336),
                Color(0xFFF44336).copy(alpha = 0.1f)
            )
            LogEntryType.OPTIMIZING -> Triple(
                Icons.Rounded.Speed,
                Color(0xFF2196F3),
                Color(0xFF2196F3).copy(alpha = 0.1f)
            )
            LogEntryType.START -> Triple(
                Icons.Rounded.PlayArrow,
                Color(0xFF9C27B0),
                Color(0xFF9C27B0).copy(alpha = 0.1f)
            )
            LogEntryType.COMPLETE -> Triple(
                Icons.Rounded.CheckCircle,
                Color(0xFF4CAF50),
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
            LogEntryType.CANCELLED -> Triple(
                Icons.Rounded.Cancel,
                Color(0xFFFF5722),
                Color(0xFFFF5722).copy(alpha = 0.1f)
            )
            else -> Triple(
                Icons.Rounded.Info,
                Color(0xFF607D8B),
                Color(0xFF607D8B).copy(alpha = 0.1f)
            )
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color
            )
        }

        Spacer(Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            entry.packageName?.let { pkg ->
                Text(
                    text = pkg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Timestamp
        Text(
            text = timeFormatter.format(Date(entry.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * Current app being optimized card showing the app icon and details.
 *
 * @param packageName The package name of the app being optimized.
 * @param modifier Modifier for layout customization.
 */
@Composable
fun CurrentAppCard(
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Get app icon and label
    val appInfo = remember(packageName) {
        if (packageName.isEmpty()) return@remember null
        try {
            val pm = context.packageManager
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            AppDisplayInfo(
                icon = applicationInfo.loadIcon(pm),
                label = pm.getApplicationLabel(applicationInfo).toString()
            )
        } catch (e: Exception) {
            null
        }
    }

    AnimatedVisibility(
        visible = packageName.isNotEmpty(),
        enter = fadeIn(tween(200)) + slideInVertically { it / 2 },
        exit = fadeOut(tween(150))
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon or fallback
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appInfo?.icon != null) {
                            Image(
                                bitmap = appInfo.icon.toBitmap().asImageBitmap(),
                                contentDescription = appInfo.label,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            // Fallback icon
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Optimizing",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = appInfo?.label ?: packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
