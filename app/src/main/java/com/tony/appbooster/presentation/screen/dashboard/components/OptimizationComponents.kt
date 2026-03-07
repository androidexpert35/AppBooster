package com.tony.appbooster.presentation.screen.dashboard.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.runtime.setValue
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
 * Beautiful circular progress indicator with smooth animation.
 *
 * @param progress Current progress from 0f to 1f.
 * @param processedCount Number of apps processed so far.
 * @param totalCount Total number of apps to process.
 * @param currentApp Package name of current app being processed.
 * @param modifier Modifier for layout customization.
 */
@Suppress("unused")
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
@Suppress("unused")
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
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Rounded.CheckCircle,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            count = skippedCount,
            color = MaterialTheme.colorScheme.tertiary,
            icon = Icons.Rounded.FastForward,
            modifier = Modifier.weight(1f)
        )
        if (failedCount > 0) {
            StatChip(
                count = failedCount,
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
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to latest entry (bottom of list)
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = entries.takeLast(50), // Keep last 50 entries
                        key = { it.id }
                    ) { entry ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                initialOffsetY = { it },
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
 * Shows app icon for app-related entries and a tick mark for successfully optimized apps.
 */
@Composable
private fun ActivityLogItem(
    entry: OptimizationLogEntry
) {
    val context = LocalContext.current

    // Load app icon if package name is available
    var appIcon by remember { androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(entry.packageName) {
        entry.packageName?.let { pkg ->
            try {
                val pm = context.packageManager
                val applicationInfo = pm.getApplicationInfo(pkg, 0)
                val drawable = applicationInfo.loadIcon(pm)
                appIcon = drawable.toBitmap(width = 64, height = 64)
            } catch (_: Exception) {
                appIcon = null
            }
        }
    }

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
            LogEntryType.ANALYZING -> Triple(
                Icons.Rounded.Search,
                Color(0xFF00BCD4),
                Color(0xFF00BCD4).copy(alpha = 0.1f)
            )
            LogEntryType.NO_PROFILE -> Triple(
                Icons.Rounded.PersonOff,
                Color(0xFF78909C),
                Color(0xFF78909C).copy(alpha = 0.1f)
            )
            else -> Triple(
                Icons.Rounded.Info,
                Color(0xFF607D8B),
                Color(0xFF607D8B).copy(alpha = 0.1f)
            )
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Check if this is an app-related entry (has package name)
    val isAppEntry = entry.packageName != null
    val isSuccess = entry.type == LogEntryType.SUCCESS

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon - show app icon for app entries, or default icon for other entries
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isAppEntry && appIcon != null) Color.Transparent else color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (isAppEntry && appIcon != null) {
                // Show app icon
                Image(
                    bitmap = appIcon!!.asImageBitmap(),
                    contentDescription = entry.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else if (isAppEntry) {
                // Show package initial as fallback
                Text(
                    text = entry.packageName.substringAfterLast(".").take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            } else {
                // Show default icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
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

        // Success tick mark for optimized apps
        if (isSuccess) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Optimized",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(Modifier.width(8.dp))
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
@Suppress("unused")
@Composable
fun CurrentAppCard(
    packageName: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = packageName.isNotEmpty(),
        enter = fadeIn(tween(200)) + slideInVertically { it / 2 },
        exit = fadeOut(tween(150))
    ) {
        CurrentAppCardContent(
            packageName = packageName,
            modifier = modifier
        )
    }
}

/**
 * Internal content for CurrentAppCard, separated to ensure proper recomposition.
 */
@Composable
private fun CurrentAppCardContent(
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Use remember with mutableStateOf for reliable icon loading
    var appIcon by remember { androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null) }
    var appLabel by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    LaunchedEffect(packageName) {
        if (packageName.isNotEmpty()) {
            try {
                val pm = context.packageManager
                val applicationInfo = pm.getApplicationInfo(packageName, 0)
                val drawable = applicationInfo.loadIcon(pm)
                appIcon = drawable.toBitmap(width = 96, height = 96)
                appLabel = pm.getApplicationLabel(applicationInfo).toString()
            } catch (_: Exception) {
                appIcon = null
                appLabel = null
            }
        } else {
            appIcon = null
            appLabel = null
        }
    }

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
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon!!.asImageBitmap(),
                            contentDescription = appLabel,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        // Fallback - show a loading indicator briefly then app initial
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = packageName.substringAfterLast(".").take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                    text = appLabel ?: packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() },
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
