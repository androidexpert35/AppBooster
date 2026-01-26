package com.tony.appbooster.domain.model.common

/**
 * Represents the full result of a single shell command execution.
 *
 * Business purpose:
 * - Allows higher layers to distinguish "command unsupported / failed" from
 *   "command succeeded but produced no output".
 * - Enables more reliable system-truth checks (e.g., `cmd package compile --check`).
 *
 * @property exitCode Process exit code returned by the shell service.
 * @property stdout Standard output captured from the command.
 * @property stderr Standard error captured from the command.
 */
data class ShellCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    /**
     * @return True when [exitCode] equals 0.
     */
    val isSuccess: Boolean get() = exitCode == 0
}
