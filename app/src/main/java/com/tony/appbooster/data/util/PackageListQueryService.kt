package com.tony.appbooster.data.util

import com.tony.appbooster.data.util.PackageListQueryService.Companion.SELF_PACKAGE_NAME
import com.tony.appbooster.domain.client.AdbShellDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries installed packages from the device via shell commands.
 *
 * Encapsulates the package listing commands and parsing logic so repository
 * classes can stay focused on orchestration.
 *
 * @property shellDataSource Data source that executes shell commands.
 * @property logger Shared logger for diagnostic output.
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
     * Uses user 0 explicitly because Android devices with multiple users
     * (Samsung One UI, work profiles, cloned apps, etc.) can reject shell
     * access to secondary users.
     *
     * @return List of normalised package names, or an empty list on failure.
     */
    suspend fun queryInstalledPackages(): List<String> {
        val command = "pm list packages --user 0"

        logger.addLog("> $command")

        val result = shellDataSource.executeCommand(command)

        return result.fold(
            onSuccess = { output ->
                val packages = parsePackageLines(output)

                if (packages.isNotEmpty()) {
                    logger.addLog("Found ${packages.size} packages")
                    return@fold packages
                }

                logger.addLog("pm list packages --user 0 returned no parsable packages.")
                logger.addLog("Raw output length: ${output.length} chars")
                logger.addLog("Preview: ${output.take(OUTPUT_PREVIEW_LENGTH).replace("\n", "\\n")}")
                logger.addLog("Trying alternative: pm list packages -3 --user 0")

                queryAlternativePackageList()
            },
            onFailure = { throwable ->
                logger.addLog("Failed to query installed packages: ${throwable.message}")
                emptyList()
            }
        )
    }

    /**
     * Fallback query using third-party packages only.
     *
     * @return List of normalised package names, or an empty list on failure.
     */
    private suspend fun queryAlternativePackageList(): List<String> {
        val command = "pm list packages -3 --user 0"

        logger.addLog("> $command")

        val result = shellDataSource.executeCommand(command)

        return result.fold(
            onSuccess = { output ->
                val packages = parsePackageLines(output)

                if (packages.isNotEmpty()) {
                    logger.addLog("Alternative command found ${packages.size} packages")
                } else {
                    logger.addLog("Alternative command also returned no packages")
                    logger.addLog(
                        "Output preview: ${output.take(OUTPUT_PREVIEW_LENGTH).replace("\n", "\\n")}"
                    )
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
     * Parses raw package manager output into a deduplicated list of valid
     * package names, excluding this application.
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
     * @param line Trimmed package manager output line.
     * @return Extracted package name or null.
     */
    private fun extractPackageName(line: String): String? = when {
        line.startsWith("package:") ->
            line.removePrefix("package:").trim().ifEmpty { null }

        line.contains(".") &&
            !line.contains(" ") &&
            !line.contains("=") ->
            line

        else -> null
    }
}
