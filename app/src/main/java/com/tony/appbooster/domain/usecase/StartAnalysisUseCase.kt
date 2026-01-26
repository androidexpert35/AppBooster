package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import javax.inject.Inject

/**
 * Starts an analysis run by ensuring connectivity and enqueuing foreground work.
 *
 * Business purpose:
 * - Guarantees that analysis starts in exactly the same way from every entry point
 *   (Analyze button, optimization prerequisite, auto-start, etc.).
 * - Centralizes connection preconditions and worker scheduling.
 *
 * @property connectAdbUseCase Use case that establishes ADB/Shizuku connectivity.
 * @property startAnalysisWorkUseCase Use case that schedules analysis work.
 */
class StartAnalysisUseCase @Inject constructor(
    private val connectAdbUseCase: ConnectAdbUseCase,
    private val startAnalysisWorkUseCase: StartAnalysisWorkUseCase
) {

    /**
     * Starts analysis for the given optimization mode.
     *
     * @param mode Optimization mode used for analysis criteria.
     * @return [Resource.Success] when the work is scheduled, [Resource.Error] otherwise.
     */
    suspend operator fun invoke(mode: AppOptimizationType): Resource<Unit> {
        return when (val connection = connectAdbUseCase()) {
            is Resource.Success -> startAnalysisWorkUseCase(mode)
            is Resource.Error -> connection
        }
    }
}
