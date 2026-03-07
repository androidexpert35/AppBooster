package com.tony.appbooster.presentation.screen.dashboard.components

import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.OptimizationProgress

/**
 * Represents the two in-progress states that drive [ProcessProgressContent]:
 * an active optimization run and an active analysis scan.
 *
 * Encapsulating each variant here keeps [ProcessProgressContent] free from
 * domain types and raw string interpolation.
 *
 * @property title Primary headline displayed at the top of the progress card.
 * @property subtitle Secondary line showing e.g. "10 / 50 apps".
 * @property progress Fractional progress from 0f to 1f.
 * @property currentPackage Package name currently being processed, empty if none.
 */
sealed interface ProcessProgressState {

    val title: String
    val subtitle: String
    val progress: Float
    val currentPackage: String

    /**
     * An optimization run is actively processing apps.
     *
     * @property title Headline for the optimization phase.
     * @property subtitle Fractional count label, e.g. "12 / 45 apps".
     * @property progress Fractional progress from 0f to 1f.
     * @property currentPackage Package currently being compiled.
     */
    data class Optimizing(
        override val title: String,
        override val subtitle: String,
        override val progress: Float,
        override val currentPackage: String
    ) : ProcessProgressState

    /**
     * An analysis scan is actively inspecting installed apps.
     *
     * @property title Headline for the scanning phase.
     * @property subtitle Fractional count label, or a generic "checking…" string.
     * @property progress Fractional progress from 0f to 1f.
     * @property currentPackage Package currently being inspected.
     */
    data class Scanning(
        override val title: String,
        override val subtitle: String,
        override val progress: Float,
        override val currentPackage: String
    ) : ProcessProgressState

    companion object {
        /**
         * Constructs an [Optimizing] state from a domain [OptimizationProgress].
         *
         * @param progress Live optimization progress from the domain layer.
         * @param titleText Localised headline string.
         * @return [Optimizing] state ready for [ProcessProgressContent].
         */
        fun fromOptimizationProgress(
            progress: OptimizationProgress,
            titleText: String
        ): Optimizing = Optimizing(
            title = titleText,
            subtitle = "${progress.processedCount} / ${progress.totalCount} apps",
            progress = progress.progress,
            currentPackage = progress.currentAppPackage
        )

        /**
         * Constructs a [Scanning] state from a domain [OptimizationAnalysis].
         *
         * @param analysis Live analysis state from the domain layer.
         * @param titleText Localised headline string.
         * @param subtitleText Localised subtitle when total app count is unknown.
         * @return [Scanning] state ready for [ProcessProgressContent].
         */
        fun fromOptimizationAnalysis(
            analysis: OptimizationAnalysis,
            titleText: String,
            subtitleText: String
        ): Scanning = Scanning(
            title = titleText,
            subtitle = if (analysis.totalAppsToScan > 0)
                "${analysis.totalAppsScanned} / ${analysis.totalAppsToScan} apps"
            else
                subtitleText,
            progress = analysis.progress,
            currentPackage = analysis.currentPackage
        )
    }
}
