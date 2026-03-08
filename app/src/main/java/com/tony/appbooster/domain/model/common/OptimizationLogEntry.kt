package com.tony.appbooster.domain.model.common

/**
 * Represents a single log entry during optimization with rich metadata
 * for beautiful UI rendering.
 *
 * @property id Unique identifier for this entry.
 * @property timestamp When this entry was created.
 * @property type The type of log entry for styling.
 * @property packageName Package name if this entry relates to a specific app.
 * @property messageKey Semantic key resolved to a localized string at the presentation layer.
 *   When null, [message] is displayed as-is (fallback for dynamic or untranslatable content).
 * @property message Human-readable fallback message used when [messageKey] is null.
 * @property detail Optional additional detail text.
 */
data class OptimizationLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogEntryType,
    val packageName: String? = null,
    val messageKey: LogMessageKey? = null,
    val message: String = "",
    val detail: String? = null
)

/**
 * Types of log entries for visual differentiation.
 */
enum class LogEntryType {
    /** Informational message */
    INFO,
    /** App optimization started */
    OPTIMIZING,
    /** App successfully optimized */
    SUCCESS,
    /** App was skipped (already optimized) */
    SKIPPED,
    /** Error or failure */
    ERROR,
    /** Process started */
    START,
    /** Process completed */
    COMPLETE,
    /** Process cancelled */
    CANCELLED,
    /** Command being executed */
    COMMAND,
    /** Analyzing apps */
    ANALYZING,
    /** App has no runtime profile (never used by the user) */
    NO_PROFILE
}

/**
 * Semantic message keys for structured log entries that require localisation.
 *
 * Each variant maps to a string resource in the presentation layer so the
 * data layer remains free of Android `Context` dependencies.
 */
enum class LogMessageKey {
    OPTIMIZATION_CANCELLED,
    ANALYSIS_CANCELLED,
    STARTING_OPTIMIZATION,
    NO_PACKAGES_FOUND,
    FORCE_MODE,
    OPTIMIZING_APP,
    OPTIMIZED,
    OPTIMIZATION_FAILED_APP,
    OPTIMIZATION_FAILED,
    OPTIMIZATION_COMPLETE,
    ALL_APPS_OPTIMIZED,
    USING_CACHED_ANALYSIS,
    STARTING_ANALYSIS,
    FOUND_APPS,
    ANALYZING_APPS,
    ANALYSIS_FAILED,
    ANALYSIS_COMPLETE,
    NEEDS_OPTIMIZATION,
    ALREADY_OPTIMIZED,
    OPTIMAL,
    NO_PROFILE_NEVER_USED,
    MODE_INFO,
}

