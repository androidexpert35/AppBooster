package com.tony.appbooster.presentation.screen.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.common.LogEntryType
import com.tony.appbooster.domain.model.common.LogMessageKey
import com.tony.appbooster.domain.model.common.OptimizationLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Scrollable activity feed that displays recent optimization log entries.
 *
 * Automatically scrolls to the latest entry as new items arrive. Shows an
 * expressive empty state when no entries are present.
 *
 * @param entries Log entries to display; only the last 50 are rendered for performance.
 * @param modifier Modifier for layout customization.
 * @param isExpanded When true the feed fills all available vertical space; otherwise
 *   it is constrained to a fixed 200 dp height.
 * @param fillHeight When true the inner [LazyColumn] uses [Modifier.weight] to grow
 *   into all remaining vertical space instead of being capped at a fixed max height.
 *   Pass `true` in the tablet two-pane layout where the card already fills the full
 *   pane height and no external scroll container exists.
 * @param applyInternalPadding When true (default) applies 20 dp horizontal padding
 *   around the card. Pass `false` when the caller manages its own padding.
 */
@Composable
fun OptimizationActivityFeed(
    entries: List<OptimizationLogEntry>,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    fillHeight: Boolean = false,
    applyInternalPadding: Boolean = true,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to latest entry whenever the list grows
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (applyInternalPadding) Modifier.padding(horizontal = 20.dp) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .then(if (isExpanded || fillHeight) Modifier.fillMaxSize() else Modifier)
        ) {
            // Header row
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
                    text = stringResource(R.string.activity_feed_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            if (entries.isEmpty()) {
                EmptyFeedState(isExpanded = isExpanded || fillHeight)
            } else {
                // fillHeight: grow into all remaining space (tablet full-height pane).
                // isExpanded (non-fill): cap at 400 dp so phone layout doesn't overflow.
                // default: fixed 200 dp collapsed view.
                val listModifier = when {
                    fillHeight -> Modifier.weight(1f)
                    isExpanded -> Modifier.heightIn(max = 400.dp)
                    else -> Modifier.height(200.dp)
                }
                LazyColumn(
                    state = listState,
                    modifier = listModifier,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = entries.takeLast(50),
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

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Empty state displayed when [OptimizationActivityFeed] has no log entries.
 */
@Composable
private fun EmptyFeedState(isExpanded: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isExpanded) Modifier.heightIn(min = 200.dp) else Modifier.height(120.dp)),
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
                text = stringResource(R.string.activity_feed_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.activity_feed_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Single row in the activity feed with expressive styling derived from the entry type.
 *
 * Shows the app icon when a package name is available, falling back to a coloured
 * initial. A green tick is appended for successfully optimized entries.
 *
 * @param entry Log entry to render.
 */
@Composable
private fun ActivityLogItem(entry: OptimizationLogEntry) {
    val context = LocalContext.current

    var appIcon by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

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

    val resolvedMessage = entry.messageKey?.let { resolveLogMessageKey(it) } ?: entry.message
    val style = remember(entry.type) { resolveLogEntryStyle(entry.type) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val isAppEntry = entry.packageName != null
    val isSuccess = entry.type == LogEntryType.SUCCESS

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(style.backgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon area
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isAppEntry && appIcon != null) Color.Transparent
                    else style.color.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isAppEntry && appIcon != null -> Image(
                    bitmap = appIcon!!.asImageBitmap(),
                    contentDescription = entry.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
                isAppEntry -> Text(
                    // Fallback to package initial when icon has not loaded yet
                    text = entry.packageName.substringAfterLast(".").take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = style.color
                )
                else -> Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = style.color
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resolvedMessage,
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

        // Success indicator for optimized apps
        if (isSuccess) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.activity_feed_optimized_cd),
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = timeFormatter.format(Date(entry.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * Resolves a [LogMessageKey] to a localised string using the current composition context.
 *
 * @param key Semantic message key emitted by the data layer.
 * @return Localised human-readable string for display.
 */
@Composable
private fun resolveLogMessageKey(key: LogMessageKey): String = when (key) {
    LogMessageKey.OPTIMIZATION_CANCELLED -> stringResource(R.string.log_optimization_cancelled)
    LogMessageKey.ANALYSIS_CANCELLED -> stringResource(R.string.log_analysis_cancelled)
    LogMessageKey.STARTING_OPTIMIZATION -> stringResource(R.string.log_starting_optimization)
    LogMessageKey.NO_PACKAGES_FOUND -> stringResource(R.string.log_no_packages_found)
    LogMessageKey.FORCE_MODE -> stringResource(R.string.log_force_mode)
    LogMessageKey.OPTIMIZING_APP -> stringResource(R.string.log_optimizing_app)
    LogMessageKey.OPTIMIZED -> stringResource(R.string.log_optimized)
    LogMessageKey.OPTIMIZATION_FAILED_APP -> stringResource(R.string.log_optimization_failed_app)
    LogMessageKey.OPTIMIZATION_FAILED -> stringResource(R.string.log_optimization_failed)
    LogMessageKey.OPTIMIZATION_COMPLETE -> stringResource(R.string.log_optimization_complete)
    LogMessageKey.ALL_APPS_OPTIMIZED -> stringResource(R.string.log_all_apps_optimized)
    LogMessageKey.USING_CACHED_ANALYSIS -> stringResource(R.string.log_using_cached_analysis)
    LogMessageKey.STARTING_ANALYSIS -> stringResource(R.string.log_starting_analysis)
    LogMessageKey.FOUND_APPS -> stringResource(R.string.log_found_apps)
    LogMessageKey.ANALYZING_APPS -> stringResource(R.string.log_analyzing_apps)
    LogMessageKey.ANALYSIS_FAILED -> stringResource(R.string.log_analysis_failed)
    LogMessageKey.ANALYSIS_COMPLETE -> stringResource(R.string.log_analysis_complete)
    LogMessageKey.NEEDS_OPTIMIZATION -> stringResource(R.string.log_needs_optimization)
    LogMessageKey.ALREADY_OPTIMIZED -> stringResource(R.string.log_already_optimized)
    LogMessageKey.OPTIMAL -> stringResource(R.string.log_optimal)
    LogMessageKey.NO_PROFILE_NEVER_USED -> stringResource(R.string.log_no_profile_never_used)
    LogMessageKey.MODE_INFO -> stringResource(R.string.log_mode_info)
}

/**
 * Maps a [LogEntryType] to its corresponding [LogEntryStyle] visual tokens.
 *
 * Centralises all colour and icon decisions in one pure function so the
 * composable body stays free of visual-token `when` branches.
 *
 * @param type The log entry type to resolve.
 * @return Resolved [LogEntryStyle] for the given type.
 */
private fun resolveLogEntryStyle(type: LogEntryType): LogEntryStyle {
    val color = when (type) {
        LogEntryType.SUCCESS, LogEntryType.COMPLETE -> Color(0xFF4CAF50)
        LogEntryType.SKIPPED -> Color(0xFFFF9800)
        LogEntryType.ERROR -> Color(0xFFF44336)
        LogEntryType.OPTIMIZING -> Color(0xFF2196F3)
        LogEntryType.START -> Color(0xFF9C27B0)
        LogEntryType.CANCELLED -> Color(0xFFFF5722)
        LogEntryType.ANALYZING -> Color(0xFF00BCD4)
        LogEntryType.NO_PROFILE -> Color(0xFF78909C)
        else -> Color(0xFF607D8B)
    }
    val icon = when (type) {
        LogEntryType.SUCCESS, LogEntryType.COMPLETE -> Icons.Rounded.CheckCircle
        LogEntryType.SKIPPED -> Icons.Rounded.FastForward
        LogEntryType.ERROR -> Icons.Rounded.Error
        LogEntryType.OPTIMIZING -> Icons.Rounded.Speed
        LogEntryType.START -> Icons.Rounded.PlayArrow
        LogEntryType.CANCELLED -> Icons.Rounded.Cancel
        LogEntryType.ANALYZING -> Icons.Rounded.Search
        LogEntryType.NO_PROFILE -> Icons.Rounded.PersonOff
        else -> Icons.Rounded.Info
    }
    return LogEntryStyle(icon = icon, color = color, backgroundColor = color.copy(alpha = 0.1f))
}

