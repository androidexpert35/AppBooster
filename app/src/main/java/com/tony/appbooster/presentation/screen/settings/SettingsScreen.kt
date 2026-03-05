package com.tony.appbooster.presentation.screen.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.schedapp.presentation.viewmodel.base.UIState
import com.example.schedapp.presentation.viewmodel.base.UIStatus
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import com.tony.appbooster.presentation.screen.common.basescreen.AppBaseScreen
import com.tony.appbooster.presentation.viewmodel.settings.SettingsUiState
import com.tony.appbooster.presentation.viewmodel.settings.SettingsViewModel

/**
 * Entry point composable for the Settings screen. It wires the Hilt-provided
 * [SettingsViewModel] into the base screen wrapper and delegates concrete
 * rendering to the internal content composable.
 *
 * @param viewModel The ViewModel exposing settings state and actions.
 * @return Unit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()

    AppBaseScreen(
        uiState = uiState.value
    ) { data ->
        SettingsScreenContent(
            data = data,
            onOptimizationTypeClick = { newType ->
                viewModel.onOptimizationTypeSelected(newType)
            }
        )
    }
}

/**
 * Renders the visual structure of the Settings screen using the provided
 * [SettingsUiState], allowing the user to inspect and change optimization
 * mode as well as view the current Shizuku status.
 *
 * @param data The current UI state snapshot for the Settings screen.
 * @param onOptimizationTypeClick Callback invoked when user requests a new optimization type.
 * @return Unit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    data: SettingsUiState,
    onOptimizationTypeClick: (AppOptimizationType) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_top_bar_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Optimization Mode Section
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_section_optimization_title),
                    subtitle = stringResource(R.string.settings_section_optimization_subtitle)
                ) {
                    OptimizationTypeSelector(
                        selectedType = data.appOptimizationType,
                        onTypeSelected = onOptimizationTypeClick
                    )
                }
            }

            // Shizuku Status Section
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_section_shizuku_title),
                    subtitle = stringResource(R.string.settings_section_shizuku_subtitle)
                ) {
                    ShizukuStatusCard(shizukuState = data.shizukuState)
                }
            }

            // About Section
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_section_about_title),
                    subtitle = stringResource(R.string.settings_section_about_subtitle)
                ) {
                    AboutCard(
                        versionName = data.appVersionName,
                        versionChannel = data.appVersionChannel
                    )
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Section header with title and subtitle for visual grouping.
 */
@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
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
        content()
    }
}

/**
 * Expressive optimization type selector with visual cards.
 */
@Composable
private fun OptimizationTypeSelector(
    selectedType: AppOptimizationType,
    onTypeSelected: (AppOptimizationType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OptimizationTypeCard(
            title = stringResource(R.string.settings_opt_speed_title),
            description = stringResource(R.string.settings_opt_speed_description),
            icon = Icons.Outlined.Speed,
            isSelected = selectedType == AppOptimizationType.SPEED_PROFILE,
            onClick = { onTypeSelected(AppOptimizationType.SPEED_PROFILE) }
        )

        OptimizationTypeCard(
            title = stringResource(R.string.settings_opt_full_title),
            description = stringResource(R.string.settings_opt_full_description),
            icon = Icons.Outlined.Bolt,
            isSelected = selectedType == AppOptimizationType.FULL_OPTIMIZATION,
            onClick = { onTypeSelected(AppOptimizationType.FULL_OPTIMIZATION) }
        )
    }
}

/**
 * Individual optimization type card with selection state animation.
 */
@Composable
private fun OptimizationTypeCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "containerColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "borderColor"
    )

    val iconContainerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "iconContainerColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "iconColor"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "elevation"
    )

    val selectionContentDescription = stringResource(R.string.settings_optimization_mode_cd, title)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = selectionContentDescription },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated icon container
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = iconContainerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = iconColor
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Animated checkmark
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "checkmark"
            ) { selected ->
                if (selected) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.settings_selected_cd),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

/**
 * Shizuku status card showing the current state of the Shizuku service.
 */
@Composable
private fun ShizukuStatusCard(shizukuState: ShizukuState) {
    val (statusIcon, statusColor, containerColor, title, description) = when (shizukuState) {
        ShizukuState.Ready -> StatusInfo(
            icon = Icons.Rounded.CheckCircle,
            iconColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            title = stringResource(R.string.settings_status_ready_title),
            description = stringResource(R.string.settings_status_ready_description)
        )
        ShizukuState.PermissionRequired -> StatusInfo(
            icon = Icons.Rounded.Warning,
            iconColor = MaterialTheme.colorScheme.tertiary,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            title = stringResource(R.string.settings_status_permission_required_title),
            description = stringResource(R.string.settings_status_permission_required_description)
        )
        ShizukuState.NotRunning -> StatusInfo(
            icon = Icons.Rounded.Warning,
            iconColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            title = stringResource(R.string.settings_status_not_running_title),
            description = stringResource(R.string.settings_status_not_running_description)
        )
        ShizukuState.NotInstalled -> StatusInfo(
            icon = Icons.Rounded.Error,
            iconColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            title = stringResource(R.string.settings_status_not_installed_title),
            description = stringResource(R.string.settings_status_not_installed_description)
        )
        is ShizukuState.Error -> StatusInfo(
            icon = Icons.Rounded.Error,
            iconColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            title = stringResource(R.string.error_generic_title),
            description = shizukuState.message
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status icon container
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = containerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = statusColor
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_shizuku_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Data class holding status display information for Shizuku states.
 */
private data class StatusInfo(
    val icon: ImageVector,
    val iconColor: Color,
    val containerColor: Color,
    val title: String,
    val description: String
)

/**
 * About card showing app version information.
 */
@Composable
private fun AboutCard(
    versionName: String,
    versionChannel: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.about_app_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = versionName.ifEmpty { stringResource(R.string.about_version_fallback) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    versionChannel?.let { channel ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = channel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview of [SettingsScreenContent] in light mode for design validation.
 *
 * @return Unit.
 */
@Preview(showBackground = true, name = "Settings - Light")
@Composable
fun SettingsScreenContentLightPreview() {
    val uiState = SettingsUiState(
        appOptimizationType = AppOptimizationType.SPEED_PROFILE,
        appVersionName = "1.0.0",
        appVersionChannel = "Alpha",
        shizukuState = ShizukuState.Ready
    )
    val baseState = UIState(
        status = UIStatus.SUCCESS,
        data = uiState
    )
    AppBaseScreen(uiState = baseState) { data ->
        SettingsScreenContent(
            data = data,
            onOptimizationTypeClick = {}
        )
    }
}

/**
 * Preview of [SettingsScreenContent] in dark mode for design validation.
 *
 * @return Unit.
 */
@Preview(showBackground = true, name = "Settings - Dark")
@Composable
fun SettingsScreenContentDarkPreview() {
    val uiState = SettingsUiState(
        appOptimizationType = AppOptimizationType.FULL_OPTIMIZATION,
        appVersionName = "1.0.0",
        appVersionChannel = "Beta",
        shizukuState = ShizukuState.NotRunning
    )
    val baseState = UIState(
        status = UIStatus.SUCCESS,
        data = uiState
    )
    AppBaseScreen(uiState = baseState) { data ->
        SettingsScreenContent(
            data = data,
            onOptimizationTypeClick = {}
        )
    }
}
