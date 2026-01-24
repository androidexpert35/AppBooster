package com.tony.appbooster.wear.data.client

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shell client that executes commands using root (su) privileges.
 *
 * This provides the simplest path to shell access for rooted watches,
 * requiring no setup or pairing. Commands run with full system privileges.
 *
 * Detection flow:
 * 1. Check if su binary exists
 * 2. Attempt to run a test command
 * 3. If successful, root is available
 *
 * @property ioDispatcher Dispatcher for IO operations.
 */
@Singleton
class RootShellClient @Inject constructor(
    private val ioDispatcher: CoroutineDispatcher
) {
    @Volatile
    private var rootAvailable: Boolean? = null

    /**
     * Checks if root access is available on this device.
     *
     * Caches the result after first check for performance.
     *
     * @return True if su is available and grants access.
     */
    suspend fun isRootAvailable(): Boolean = withContext(ioDispatcher) {
        rootAvailable?.let { return@withContext it }

        val available = checkRootAccess()
        rootAvailable = available
        available
    }

    /**
     * Executes a shell command with root privileges.
     *
     * @param command The command to execute.
     * @return Result containing command output on success, or exception on failure.
     * @throws IllegalStateException If root access is not available.
     */
    suspend fun execute(command: String): Result<String> = withContext(ioDispatcher) {
        runCatching {
            if (rootAvailable == false) {
                throw IllegalStateException("Root access not available")
            }

            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))

            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                output.trim()
            } else {
                throw IOException("Command failed (exit $exitCode): ${error.ifEmpty { output }}")
            }
        }
    }

    /**
     * Executes multiple commands in sequence with root privileges.
     *
     * @param commands List of commands to execute.
     * @return Result containing list of outputs, or first error encountered.
     */
    suspend fun executeAll(commands: List<String>): Result<List<String>> = withContext(ioDispatcher) {
        runCatching {
            commands.map { command ->
                execute(command).getOrThrow()
            }
        }
    }

    /**
     * Forces a re-check of root availability.
     *
     * Use this after user grants root permission in a root manager app.
     */
    suspend fun refreshRootStatus(): Boolean {
        rootAvailable = null
        return isRootAvailable()
    }

    /**
     * Performs the actual root access check.
     */
    private fun checkRootAccess(): Boolean {
        return try {
            // First check if su binary exists
            val whichProcess = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val whichResult = whichProcess.inputStream.bufferedReader().readText().trim()
            whichProcess.waitFor()

            if (whichResult.isEmpty()) {
                return false
            }

            // Try to actually run a command with su
            val testProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val testOutput = testProcess.inputStream.bufferedReader().readText()
            val exitCode = testProcess.waitFor()

            // Check if we got root (uid=0)
            exitCode == 0 && testOutput.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "RootShellClient"
    }
}
