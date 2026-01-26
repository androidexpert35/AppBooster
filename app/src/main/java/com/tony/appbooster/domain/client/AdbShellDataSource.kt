package com.tony.appbooster.domain.client

import com.tony.appbooster.domain.model.common.ShellCommandResult
import kotlinx.coroutines.flow.Flow

/**
 * Data source responsible for executing shell commands over an already
 * established ADB client connection and streaming their output.
 */
interface AdbShellDataSource {

    /**
     * Executes the provided shell command on the connected device and returns
     * the full textual output once the command completes.
     *
     * @param command Shell command to be executed on the remote device.
     * @return [Result] containing the output or a failure when the command
     * cannot be executed or the client disconnects.
     */
    suspend fun executeCommand(
        command: String
    ): Result<String>

    /**
     * Executes the provided shell command and exposes a live stream of output
     * lines as the command runs, useful for long running compile sessions.
     *
     * @param command Shell command to be executed on the remote device.
     * @return [kotlinx.coroutines.flow.Flow] emitting output lines in real time wrapped in [Result]
     * so that IO errors can be surfaced.
     */
    suspend fun streamCommand(
        command: String
    ): Flow<Result<String>>

    /**
     * Executes the provided shell command and returns full process details.
     *
     * Business purpose:
     * - Enables robust feature logic that depends on command support/exit codes.
     * - Avoids misclassifying "unsupported command" as "app needs optimization".
     *
     * @param command Shell command to be executed on the remote device.
     * @return [Result] containing [ShellCommandResult] on success.
     */
    suspend fun executeCommandDetailed(
        command: String
    ): Result<ShellCommandResult>
}