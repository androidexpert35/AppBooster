package com.tony.appbooster.data.client

import android.util.Log
import com.tony.appbooster.domain.client.AdbShellClient
import com.tony.appbooster.domain.client.ShizukuShellClient
import com.tony.appbooster.domain.model.common.ShellCommandResult
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shizuku-based implementation of [AdbShellClient].
 *
 * Uses the Shizuku service to execute shell commands with elevated (shell UID) privileges.
 * This implementation delegates all operations to [ShizukuShellClient] which handles
 * the Shizuku lifecycle and permission management.
 *
 * **How it works:**
 * Shizuku runs a privileged server process (started via ADB or root) that accepts
 * Binder IPC calls from authorized apps. Commands are executed with the same
 * privileges as ADB shell, enabling system-level operations.
 *
 * **Setup required:**
 * 1. User installs Shizuku app
 * 2. User starts Shizuku service via ADB command or root
 * 3. User grants this app permission through Shizuku's UI
 */
@Singleton
class AdbShellClientImpl @Inject constructor(
    private val shizukuClient: ShizukuShellClient
) : AdbShellClient {

    override suspend fun isConnected(): Boolean {
        shizukuClient.refreshState()
        return shizukuClient.isReady()
    }

    override suspend fun ensureConnected() {
        shizukuClient.refreshState()

        val currentState = shizukuClient.state.first()

        when (currentState) {
            ShizukuState.Ready -> {
                // Intentionally no-op: higher layers (repository/UI) handle user-facing logging.
            }
            ShizukuState.NotInstalled -> {
                throw IllegalStateException(
                    "Shizuku is not installed. Please install Shizuku from the Play Store or shizuku.rikka.app"
                )
            }
            ShizukuState.NotRunning -> {
                throw IllegalStateException(
                    "Shizuku service is not running. Please start it via shizuku app"
                )
            }
            ShizukuState.PermissionRequired -> {
                throw IllegalStateException(
                    "Shizuku permission required. Please grant permission in the Shizuku app."
                )
            }
            is ShizukuState.Error -> {
                throw IllegalStateException(
                    "Shizuku error: ${currentState.message}"
                )
            }
        }
    }

    override suspend fun executeDetailed(command: String): ShellCommandResult {
        ensureConnected()

        // Do not log commands here to avoid duplicated logs; repositories already log "> <command>".

        val result = shizukuClient.execute(command)

        if (!result.isSuccess) {
            Log.e(TAG, "Command failed with exit code ${result.exitCode}: ${result.error}")
        }

        return ShellCommandResult(
            exitCode = result.exitCode,
            stdout = result.output,
            stderr = result.error
        )
    }

    override suspend fun execute(command: String): String {
        // Preserve existing behavior for callers that only need stdout.
        return executeDetailed(command).stdout
    }

    override fun stream(command: String): Flow<Result<String>> {
        return shizukuClient.executeStreaming(command)
    }

    companion object {
        private const val TAG = "AdbShellClientShizuku"
    }
}
