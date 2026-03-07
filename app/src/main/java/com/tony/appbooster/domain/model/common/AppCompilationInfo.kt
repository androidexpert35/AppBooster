package com.tony.appbooster.domain.model.common

/**
 * Contains compilation status information for a single installed application.
 *
 * This data is extracted from `dumpsys package` output and oat/odex file metadata
 * to determine whether an app has been optimized and when the last compilation occurred.
 *
 * @property packageName Unique package identifier of the application.
 * @property compilerFilter Current compilation filter applied (e.g., "speed", "speed-profile", "verify").
 * @property lastCompilationTimeMs Timestamp (epoch millis) when dex2oat was last run for this app.
 *           Null if the timestamp could not be determined (e.g., no odex file exists).
 * @property lastUpdateTimeMs Timestamp (epoch millis) when the app was last updated/installed.
 *           Used to detect if re-optimization is needed after an app update.
 * @property oatFileExists Whether the compiled oat/odex file exists on disk.
 * @property skipReason Human-readable reason why this app was skipped, null if needs optimization.
 * @property needsOptimization Pre-computed flag indicating whether this app should be optimized.
 */
data class AppCompilationInfo(
    val packageName: String,
    val compilerFilter: String?,
    val lastCompilationTimeMs: Long?,
    val lastUpdateTimeMs: Long?,
    val oatFileExists: Boolean = false,
    val skipReason: SkipReason? = null,
    val needsOptimization: Boolean
) {
    /**
     * Describes why an app was skipped from optimization.
     */
    sealed interface SkipReason {
        /**
         * App was recently optimized within the threshold period.
         *
         * @property daysAgo Number of days since last optimization.
         * @property filter The compiler filter currently applied.
         */
        data class RecentlyOptimized(
            val daysAgo: Long,
            val filter: String
        ) : SkipReason

        /**
         * App already has the target optimization filter applied.
         *
         * @property filter The current compiler filter.
         */
        data class AlreadyOptimal(val filter: String) : SkipReason

        /**
         * App is a system app that shouldn't be modified.
         */
        data object SystemApp : SkipReason

        /**
         * App has no runtime profile because the user has never opened it.
         *
         * Profile-guided compilation (speed-profile) is pointless without a
         * profile, so the app is skipped. The user should open the app at
         * least once and then re-run optimization.
         *
         * @property filter The current compiler filter (typically "verify").
         */
        data class NoProfile(val filter: String) : SkipReason
    }

    companion object {
        /**
         * Minimum days since last optimization before re-optimization is recommended.
         *
         * For profile-guided optimization (speed-profile), 7 days allows time for
         * new runtime profiles to accumulate, making re-compilation worthwhile.
         */
        const val MIN_DAYS_BEFORE_REOPTIMIZATION = 7L

        /**
         * Milliseconds in one day for time calculations.
         */
        private const val MS_PER_DAY = 24 * 60 * 60 * 1000L

        /**
         * Compiler filters that indicate the app is already fully optimized.
         * Apps with these filters don't benefit from `speed` re-compilation.
         */
        private val FULLY_OPTIMIZED_FILTERS = setOf("speed", "everything")

        /**
         * Compiler filters that indicate the app has profile-guided optimization.
         * These apps can benefit from re-compilation after accumulating new profiles.
         */
        private val PROFILE_OPTIMIZED_FILTERS = setOf("speed-profile")

        /**
         * Determines whether an app needs optimization based on its current state.
         *
         * @param compilerFilter Current compiler filter, or null if unknown.
         * @param lastCompilationTimeMs Epoch millis of last compilation, or null if unknown.
         * @param lastUpdateTimeMs Epoch millis of last app update, or null if unknown.
         * @param targetFilter The optimization mode the user wants to apply.
         * @param oatFileExists Whether the oat/odex file exists.
         * @return Pair of (needsOptimization, skipReason).
         */
        fun evaluateOptimization(
            compilerFilter: String?,
            lastCompilationTimeMs: Long?,
            lastUpdateTimeMs: Long?,
            targetFilter: String,
            oatFileExists: Boolean
        ): Pair<Boolean, SkipReason?> {
            val now = System.currentTimeMillis()
            val minAgeMs = MIN_DAYS_BEFORE_REOPTIMIZATION * MS_PER_DAY

            // Case 1: No oat file exists - always optimize
            if (!oatFileExists) {
                return true to null
            }

            // Case 2: No compilation info available - always optimize
            if (compilerFilter == null || lastCompilationTimeMs == null) {
                return true to null
            }

            // Case 3: App was updated after last compilation - needs re-optimization
            if (lastUpdateTimeMs != null && lastUpdateTimeMs > lastCompilationTimeMs) {
                return true to null
            }

            // Case 3.5: "verify" filter means no runtime profile exists (user never opened the app).
            // Profile-guided compilation is pointless without a profile; skip for speed-profile mode.
            if (compilerFilter == "verify" && targetFilter == "speed-profile") {
                return false to SkipReason.NoProfile(compilerFilter)
            }

            // Case 4: Check if already has the target optimization
            val isTargetFullyOptimized = targetFilter == "speed" &&
                    compilerFilter in FULLY_OPTIMIZED_FILTERS
            val isTargetProfileOptimized = targetFilter == "speed-profile" &&
                    compilerFilter in PROFILE_OPTIMIZED_FILTERS

            if (isTargetFullyOptimized || isTargetProfileOptimized) {
                val timeSinceOptimization = now - lastCompilationTimeMs
                if (timeSinceOptimization < minAgeMs) {
                    // Optimized too recently - skip
                    val daysAgo = timeSinceOptimization / MS_PER_DAY
                    return false to SkipReason.RecentlyOptimized(daysAgo, compilerFilter)
                }
            }

            // Case 5: Was optimized long ago, or has a lower-quality filter - optimize
            return true to null
        }

        /**
         * Determines whether an app needs optimization based on its current state.
         * Legacy method for backward compatibility.
         *
         * @param compilerFilter Current compiler filter, or null if unknown.
         * @param lastCompilationTimeMs Epoch millis of last compilation, or null if unknown.
         * @param lastUpdateTimeMs Epoch millis of last app update, or null if unknown.
         * @param targetFilter The optimization mode the user wants to apply.
         * @return True if the app should be included in the optimization run.
         */
        fun shouldOptimize(
            compilerFilter: String?,
            lastCompilationTimeMs: Long?,
            lastUpdateTimeMs: Long?,
            targetFilter: String
        ): Boolean {
            return evaluateOptimization(
                compilerFilter = compilerFilter,
                lastCompilationTimeMs = lastCompilationTimeMs,
                lastUpdateTimeMs = lastUpdateTimeMs,
                targetFilter = targetFilter,
                oatFileExists = lastCompilationTimeMs != null
            ).first
        }
    }
}
