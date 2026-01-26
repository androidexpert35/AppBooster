package com.tony.appbooster.data.repository

/**
 * Parses ART/dexopt related command outputs into normalized compiler filter signals.
 *
 * Business purpose:
 * - Centralizes parsing logic so repository code stays readable.
 * - Avoids reliance on shell utilities like grep/head.
 * - Improves testability by making parsing pure and deterministic.
 */
internal object DexoptStatusParser {

    /**
     * Attempts to interpret the output of `cmd package compile --check <package>`.
     *
     * Different Android versions output different formats. We support:
     * - `true` / `false`
     * - Strings containing "compilation needed" / "compilation not needed"
     *
     * @param output Raw command output.
     * @return True if the system says compilation is needed, false if not needed, or null if unknown.
     */
    fun parseCompileCheckNeedsOptimization(output: String): Boolean? {
        if (output.isBlank()) return null

        val lower = output.trim().lowercase()

        if (lower == "true") return true
        if (lower == "false") return false

        if (lower.contains("compilation") && lower.contains("not") && lower.contains("needed")) return false
        if (lower.contains("compilation") && lower.contains("needed")) return true

        if (lower.contains("need") && lower.contains("compile")) {
            if (lower.contains("not") && lower.contains("needed")) return false
            if (lower.contains("needed")) return true
        }

        return null
    }

    /**
     * Parses compiler filter for a package from the full `dumpsys package dexopt` output.
     *
     * @param packageName Target package name.
     * @param dump Full dumpsys output.
     * @return Normalized filter string (e.g., "speed-profile", "speed", "everything") or null.
     */
    fun parseCompilerFilterFromDexoptDump(packageName: String, dump: String): String? {
        val lines = dump.lineSequence().toList()
        val idx = lines.indexOfFirst { it.contains(packageName) }
        if (idx < 0) return null

        val window = lines.subList(idx, minOf(idx + 20, lines.size))
        for (line in window) {
            val lower = line.trim().lowercase()
            parseCompilerFilterFromLine(lower)?.let { return it }
        }
        return null
    }

    /**
     * Extracts a compiler filter keyword from a single lowercased line.
     */
    fun parseCompilerFilterFromLine(lowercasedLine: String): String? {
        return when {
            lowercasedLine.contains("speed-profile") -> "speed-profile"
            lowercasedLine.contains("everything") -> "everything"
            lowercasedLine.contains("[status=speed]") || (lowercasedLine.contains("speed") && !lowercasedLine.contains("profile")) -> "speed"
            lowercasedLine.contains("quicken") -> "quicken"
            lowercasedLine.contains("verify") -> "verify"
            lowercasedLine.contains("run-from-apk") || lowercasedLine.contains("extract") -> "extract"
            else -> null
        }
    }
}
