package com.tony.appbooster.presentation.screen.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.shizuku.ShizukuState

/**
 * Card displaying the current [ShizukuState] with a contextual icon, title,
 * and description that adapt to each possible state variant.
 *
 * @param shizukuState The current runtime state of the Shizuku service.
 */
@Composable
internal fun ShizukuStatusCard(shizukuState: ShizukuState) {
    val (statusIcon, statusColor, containerColor, title, description) = when (shizukuState) {
        ShizukuState.Ready -> ShizukuStatusConfig(
            icon = Icons.Rounded.CheckCircle,
            iconColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            title = stringResource(R.string.settings_status_ready_title),
            description = stringResource(R.string.settings_status_ready_description)
        )
        ShizukuState.PermissionRequired -> ShizukuStatusConfig(
            icon = Icons.Rounded.Warning,
            iconColor = MaterialTheme.colorScheme.tertiary,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            title = stringResource(R.string.settings_status_permission_required_title),
            description = stringResource(R.string.settings_status_permission_required_description)
        )
        ShizukuState.NotRunning -> ShizukuStatusConfig(
            icon = Icons.Rounded.Warning,
            iconColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            title = stringResource(R.string.settings_status_not_running_title),
            description = stringResource(R.string.settings_status_not_running_description)
        )
        ShizukuState.NotInstalled -> ShizukuStatusConfig(
            icon = Icons.Rounded.Error,
            iconColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            title = stringResource(R.string.settings_status_not_installed_title),
            description = stringResource(R.string.settings_status_not_installed_description)
        )
        is ShizukuState.Error -> ShizukuStatusConfig(
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
 * Internal visual token bundle resolved per [ShizukuState] variant.
 * Kept private to this file as it is not part of the public component API.
 *
 * @property icon Icon representing the current Shizuku status.
 * @property iconColor Tint applied to the status icon.
 * @property containerColor Background colour of the icon surface.
 * @property title Primary status label shown to the user.
 * @property description Supplementary detail message for the status.
 */
private data class ShizukuStatusConfig(
    val icon: ImageVector,
    val iconColor: Color,
    val containerColor: Color,
    val title: String,
    val description: String
)

