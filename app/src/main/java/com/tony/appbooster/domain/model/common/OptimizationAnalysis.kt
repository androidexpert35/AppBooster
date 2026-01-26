package com.tony.appbooster.domain.model.common

/**
 * Represents the result of a pre-optimization analysis scan.
 *
 * This data is collected before optimization starts to show the user
 * how many apps need optimization vs are already optimized.
 *
 * @property totalAppsScanned Total number of installed apps that were analyzed so far.
 * @property totalAppsToScan Total number of apps to be scanned (for progress calculation).
 * @property appsNeedingOptimization Number of apps that need optimization.
 * @property appsAlreadyOptimized Number of apps that are already optimized (will be skipped).
 * @property packagesNeedingOptimization List of package names that need optimization (cached for reuse).
 * @property isScanning Whether the analysis is currently in progress.
 * @property currentPackage Package currently being analyzed, empty if not scanning.
 * @property lastScanTimeMs Timestamp of when the last scan completed, or null if never scanned.
 */
data class OptimizationAnalysis(
    val totalAppsScanned: Int = 0,
    val totalAppsToScan: Int = 0,
    val appsNeedingOptimization: Int = 0,
    val appsAlreadyOptimized: Int = 0,
    val packagesNeedingOptimization: List<String> = emptyList(),
    val isScanning: Boolean = false,
    val currentPackage: String = "",
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

    /**
     * Progress of the current scan from 0f to 1f.
     */
    val progress: Float
        get() = if (totalAppsToScan > 0) totalAppsScanned.toFloat() / totalAppsToScan else 0f
}
