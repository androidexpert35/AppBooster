package com.tony.appbooster.data.util

import com.tony.appbooster.data.util.PackageListQueryService.Companion.SELF_PACKAGE_NAME
import com.tony.appbooster.domain.client.AdbShellDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries installed packages from the device via shell commands.
 *
 * Encapsulates the `pm list packages` parsing logic so that repository
 * classes can stay focused on orchestration. Parsing is unified in a
 * single [parsePackageLines] method shared by the primary and fallback
 * commands.
 *
 * @property shellDataSource Data source that executes shell commands.
 * @property logger Shared logger for diagnostic output.
 * @constructor Creates a query service with required shell and logging dependencies.
 */
@Singleton
class PackageListQueryService @Inject constructor(
    private val shellDataSource: AdbShellDataSource,
    private val logger: OptimizationLogger
) {

    companion object {
        /** Package name of this app, excluded from optimization to prevent self-crash. */
        internal const val SELF_PACKAGE_NAME = "com.tony.appbooster"

        /** Maximum characters shown when previewing raw command output for diagnostics. */
        private const val OUTPUT_PREVIEW_LENGTH = 500
    }

    /**
     * Queries all installed package names from the device.
     *
     * Tries the standard `pm list packages` command first; if no packages
     * are parsed, falls back to `pm list packages -3` (third-party only).
     *
     * @return List of normalised package names, or an empty list on failure.
     */
    suspend fun queryInstalledPackages(): List<String> {
        val command = "pm list packages"
        logger.addLog("> $command")
        val result = shellDataSource.executeCommand(command)

        return result.fold(
            onSuccess = { output ->
                val packages = parsePackageLines(output)
                if (packages.isNotEmpty()) {
                    logger.addLog("Found ${packages.size} packages")
                    return@fold packages
                }

                // Diagnostics when primary command yields nothing
                logger.addLog("pm list packages returned no parsable packages.")
                logger.addLog("Raw output length: ${output.length} chars")
                logger.addLog("Preview: ${output.take(OUTPUT_PREVIEW_LENGTH).replace("\n", "\\n")}")
                logger.addLog("Trying alternative: pm list packages -3")

                queryAlternativePackageList()
            },
            onFailure = { throwable ->
                logger.addLog("Failed to query installed packages: ${throwable.message}")
                emptyList()
            }
        )
    }

    /**
     * Fallback query using `pm list packages -3` for third-party apps only.
     *
     * @return List of normalised package names, or an empty list on failure.
     */
    private suspend fun queryAlternativePackageList(): List<String> {
        val command = "pm list packages -3"
        logger.addLog("> $command")
        val result = shellDataSource.executeCommand(command)

        return result.fold(
            onSuccess = { output ->
                val packages = parsePackageLines(output)
                if (packages.isNotEmpty()) {
                    logger.addLog("Alternative command found ${packages.size} packages")
                } else {
                    logger.addLog("Alternative command also returned no packages")
                    logger.addLog("Output preview: ${output.take(OUTPUT_PREVIEW_LENGTH).replace("\n", "\\n")}")
                }
                packages
            },
            onFailure = { throwable ->
                logger.addLog("Alternative command failed: ${throwable.message}")
                emptyList()
            }
        )
    }

    /**
     * Parses raw `pm list packages` output into a deduplicated list of
     * valid package names, excluding [SELF_PACKAGE_NAME].
     *
     * Handles multiple line-ending formats (CRLF, LF, CR) and the
     * standard `package:<name>` prefix used by most Android versions.
     *
     * @param output Raw shell command output.
     * @return Parsed and filtered package name list.
     */
    internal fun parsePackageLines(output: String): List<String> {
        val normalised = output
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        return normalised.split("\n")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line -> extractPackageName(line) }
            .filter { pkg ->
                pkg.contains(".") &&
                    pkg != SELF_PACKAGE_NAME &&
                    !pkg.contains(SELF_PACKAGE_NAME)
            }
            .toList()
    }

    /**
     * Extracts a package name from a single output line.
     *
     * @param line Trimmed, non-empty line from `pm list packages`.
     * @return Extracted package name, or null if the line is not parseable.
     */
    private fun extractPackageName(line: String): String? = when {
        line.startsWith("package:") -> line.removePrefix("package:").trim().ifEmpty { null }
        // Some Android versions emit bare package names
        line.contains(".") && !line.contains(" ") && !line.contains("=") -> line
        else -> null
    }
}

