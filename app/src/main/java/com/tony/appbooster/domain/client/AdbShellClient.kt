package com.tony.appbooster.domain.client

import com.tony.appbooster.domain.model.common.ShellCommandResult
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for executing shell commands with elevated privileges.
 *
 * This interface provides a unified API for shell command execution,
 * abstracting away the underlying mechanism (Shizuku, root, etc.).
 * Commands are executed with shell (ADB) UID privileges, enabling
 * system-level operations like app optimization.
 */
interface AdbShellClient {

    /**
     * Checks if the shell client is ready to execute commands.
     *
     * @return True if connected and authorized, false otherwise.
     */
    suspend fun isConnected(): Boolean

    /**
     * Ensures the shell client is ready for command execution.
     *
     * For Shizuku-based implementations, this verifies Shizuku is running
     * and permission is granted. No actual "connection" is needed since
     * Shizuku uses Binder IPC.
     *
     * @throws IllegalStateException if the client cannot be prepared.
     */
    suspend fun ensureConnected()

    /**
     * Executes a single shell command and waits for the result.
     *
     * @param command The shell command (e.g., "pm list packages").
     * @return The full standard output as a String.
     * @throws IllegalStateException if not connected.
     */
    suspend fun execute(command: String): String

    /**
     * Streams the output of a long-running command line-by-line.
     *
     * @param command The shell command to execute.
     * @return A Flow emitting lines of output or a Result failure.
     */
    fun stream(command: String): Flow<Result<String>>

    /**
     * Executes a single shell command and returns detailed process information.
     *
     * Business purpose:
     * - Supports features that must differentiate between "unsupported command" and
     *   a successful execution with empty output.
     *
     * Implementations should return a non-zero [ShellCommandResult.exitCode] when the
     * command fails.
     *
     * @param command The shell command to execute.
     * @return Detailed result including exit code, stdout, and stderr.
     * @throws IllegalStateException if not connected.
     */
    suspend fun executeDetailed(command: String): ShellCommandResult
}