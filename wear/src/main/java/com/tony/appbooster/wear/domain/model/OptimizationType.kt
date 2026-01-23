package com.tony.appbooster.wear.domain.model

/**
 * Represents how the application should optimize its runtime behavior,
 * allowing the user to trade off compilation time against runtime speed.
 *
 * @property value The ART compile mode string passed to `cmd package compile`.
 */
enum class OptimizationType(val value: String) {
    /**
     * Prioritizes fast install and incremental builds over maximum runtime speed.
     * Uses profile-guided compilation based on actual app usage patterns.
     */
    SPEED_PROFILE("speed-profile"),

    /**
     * Compiles more aggressively for runtime performance at the cost of longer build times.
     * Compiles all methods ahead-of-time for maximum performance.
     */
    FULL_OPTIMIZATION("speed")
}
