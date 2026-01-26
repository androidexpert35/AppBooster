package com.tony.appbooster.presentation.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"

/**
 * Orchestrates Android 13+ notification permission requests before executing an action.
 *
 * Business purpose:
 * - Centralizes notification permission UX so screens remain focused on UI/state.
 * - Ensures a consistent flow (rationale → request → run action) across features.
 *
 * Usage pattern:
 * - Provide the `runWithNotificationPermission` lambda to your UI.
 * - Call it when the user taps “Start” and you need notifications.
 *
 * @param content Slot that receives a function to run the guarded action.
 */
@Composable
fun NotificationPermissionGate(
    content: @Composable (
        runWithNotificationPermission: (action: () -> Unit) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Regardless of grant or deny, continue with the action.
        pendingAction?.invoke()
        pendingAction = null
    }

    if (showRationale) {
        NotificationPermissionRationaleDialog(
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(POST_NOTIFICATIONS_PERMISSION)
            },
            onDismiss = {
                // User refused the rationale; still continue with the action.
                showRationale = false
                pendingAction?.invoke()
                pendingAction = null
            }
        )
    }

    LaunchedEffect(Unit) {
        // If the dialog was showing across config changes, ensure we don't keep stale state.
        if (!showRationale && pendingAction == null) return@LaunchedEffect
    }

    content { action ->
        if (NotificationPermissionManager.shouldRequest(context)) {
            pendingAction = action
            showRationale = true
        } else {
            action()
        }
    }
}
