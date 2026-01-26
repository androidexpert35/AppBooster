package com.tony.appbooster.domain.model.common

/**
 * Represents the result of a pre-optimization analysis scan.
 *
 * This data is collected before optimization starts to show the user
 * how many apps need optimization vs are already optimized.
 *
 * @property totalAppsScanned Total number of installed apps that were analyzed.
 * @property appsNeedingOptimization Number of apps that need optimization.
 * @property appsAlreadyOptimized Number of apps that are already optimized (will be skipped).
 * @property isScanning Whether the analysis is currently in progress.
 * @property lastScanTimeMs Timestamp of when the last scan completed, or null if never scanned.
 */
data class OptimizationAnalysis(
    val totalAppsScanned: Int = 0,
    val appsNeedingOptimization: Int = 0,
    val appsAlreadyOptimized: Int = 0,
    val isScanning: Boolean = false,
    val lastScanTimeMs: Long? = null
) {
    /**
     * Whether a scan has been completed at least once.
     */
    val hasScanned: Boolean
        get() = lastScanTimeMs != null

    /**
     * Whether all apps are already optimized (nothing to do).
     */
    val allOptimized: Boolean
        get() = hasScanned && appsNeedingOptimization == 0 && appsAlreadyOptimized > 0
}
