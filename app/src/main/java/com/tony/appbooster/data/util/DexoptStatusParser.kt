package com.tony.appbooster.data.util

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
     * Checks whether the given package appears in a dexopt dump at all.
     *
     * Some Android builds omit compiler-filter lines for overlay/system packages.
     * In those cases, presence alone is a useful signal that the system is aware
     * of dexopt state, even if details are not reported.
     */
    fun isPackagePresentInDexoptDump(packageName: String, dump: String): Boolean {
        if (dump.isBlank()) return false

        // Match common bracketed forms:
        // - "[com.example.app]"
        // - "Dexopt state:\n  [com.example.app]"
        // - "Dexopt state:  [com.example.app]"
        val needle = "[$packageName]"
        return dump.contains(needle)
    }

    /**
     * Parses compiler filter for a package from the full `dumpsys package dexopt` output.
     *
     * Supports multiple formats across Android versions:
     * - Explicit filter lines (compiler-filter=speed-profile)
     * - Status annotations ([status=speed])
     * - Newer builds that only list the package in a "Dexopt state" section without details
     *   (in this case returns "unknown-present").
     */
    fun parseCompilerFilterFromDexoptDump(packageName: String, dump: String): String? {
        val lines = dump.lineSequence().toList()

        // Prefer the first occurrence of the package name in bracketed form
        val bracketed = "[$packageName]"
        val idx = lines.indexOfFirst { it.contains(bracketed) || it.contains(packageName) }
        if (idx < 0) return null

        val window = lines.subList(idx, minOf(idx + 30, lines.size))
        for (line in window) {
            val lower = line.trim().lowercase()
            parseCompilerFilterFromLine(lower)?.let { return it }
        }

        // If we can see the package in a Dexopt state section but no filter lines are provided,
        // return a marker so callers can treat it differently from "not found".
        return if (isPackagePresentInDexoptDump(packageName, dump)) "unknown-present" else null
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

