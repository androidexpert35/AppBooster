package com.tony.appbooster.domain.model.common

/**
 * Represents a single log entry during optimization with rich metadata
 * for beautiful UI rendering.
 *
 * @property id Unique identifier for this entry.
 * @property timestamp When this entry was created.
 * @property type The type of log entry for styling.
 * @property packageName Package name if this entry relates to a specific app.
 * @property message Human-readable message.
 * @property detail Optional additional detail text.
 */
data class OptimizationLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogEntryType,
    val packageName: String? = null,
    val message: String,
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
    ANALYZING
}
