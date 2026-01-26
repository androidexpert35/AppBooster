package com.tony.appbooster.data.repository


import com.tony.appbooster.domain.client.AdbShellClient
import com.tony.appbooster.domain.client.AdbShellDataSource
import com.tony.appbooster.domain.model.common.ShellCommandResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Default implementation of [AdbShellDataSource] that delegates shell
 * execution to the already configured ADB client session.
 *
 * The concrete ADB client integration must be injected here so that the
 * repository can remain UI-agnostic and testable.
 *
 * @param adbClient Low-level ADB client wrapper that knows how to execute
 * shell commands and expose their output.
 */
class AdbShellDataSourceImpl @Inject constructor(
    private val adbClient: AdbShellClient
) : AdbShellDataSource {

    /**
     * Executes a command and collects all lines from the client before
     * returning them as a joined [String].
     *
     * @param command Shell command to be executed.
     * @return [Result] with full output or failure if the client fails.
     */
    override suspend fun executeCommand(
        command: String
    ): Result<String> = runCatching {
        adbClient.execute(command)
    }

    /**
     * Streams command output using the underlying client's streaming API.
     *
     * @param command Shell command to be executed.
     * @return [Flow] of [Result] where each emission is a single output line
     * or an error when the ADB transport fails mid-stream.
     */
    override suspend fun streamCommand(
        command: String
    ): Flow<Result<String>> = flow {
        adbClient.stream(command).collect { lineResult ->
            emit(lineResult)
        }
    }

    /**
     * Executes a command and returns the result as a [ShellCommandResult]
     * object, providing detailed information about the command execution.
     *
     * @param command Shell command to be executed.
     * @return [Result] with [ShellCommandResult] containing detailed output
     * or failure if the client fails.
     */
    override suspend fun executeCommandDetailed(
        command: String
    ): Result<ShellCommandResult> = runCatching {
        adbClient.executeDetailed(command)
    }
}
