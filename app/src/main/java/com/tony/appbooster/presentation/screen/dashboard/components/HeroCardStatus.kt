package com.tony.appbooster.presentation.screen.dashboard.components

/**
 * Represents every possible outcome state of the hero result card after an
 * optimization run completes or is interrupted.
 *
 * Each entry carries the data needed to drive a single, unified
 * [HeroResultPanel] composable instead of maintaining separate implementations
 * per outcome.
 *
 * @property processedCount Number of apps successfully optimized during the run.
 * @property skippedCount Number of apps skipped (already optimized recently).
 * @property totalCount Total apps targeted by the run (0 for [AllOptimized]).
 */
sealed interface HeroCardStatus {

    val processedCount: Int
    val skippedCount: Int
    val totalCount: Int

    /**
     * Optimization run finished without interruption.
     *
     * @property processedCount Apps fully optimized in this run.
     * @property skippedCount Apps skipped because they were already optimal.
     * @property totalCount Total apps that were targeted.
     */
    data class Completed(
        override val processedCount: Int,
        override val skippedCount: Int,
        override val totalCount: Int
    ) : HeroCardStatus

    /**
     * Optimization run was stopped by the user before all apps were processed.
     *
     * @property processedCount Apps optimized before cancellation.
     * @property skippedCount Apps skipped before cancellation.
     * @property totalCount Total apps that were targeted.
     */
    data class Canceled(
        override val processedCount: Int,
        override val skippedCount: Int,
        override val totalCount: Int
    ) : HeroCardStatus

    /**
     * Every targeted app was already at peak optimization; nothing needed to run.
     *
     * @property optimizedCount Apps confirmed as already optimized.
     * @property noProfileCount Apps skipped because they have no runtime profile.
     */
    data class AllOptimized(
        val optimizedCount: Int,
        val noProfileCount: Int = 0
    ) : HeroCardStatus {
        override val processedCount: Int = 0
        override val skippedCount: Int = noProfileCount
        override val totalCount: Int = optimizedCount + noProfileCount
    }
}

