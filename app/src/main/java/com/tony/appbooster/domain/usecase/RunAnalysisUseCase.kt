package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbRepository
import javax.inject.Inject

/**
 * Executes the pre-optimization analysis scan on the connected device.
 *
 * Business purpose:
 * - Provides a single orchestration entry point for analysis logic.
 * - Allows Workers and presentation to call analysis without depending on repository details.
 *
 * @property repository Repository performing analysis via shell commands.
 */
class RunAnalysisUseCase @Inject constructor(
    private val repository: AdbRepository
) {

    /**
     * Runs analysis for the given optimization mode.
     *
     * @param mode Optimization criteria used when checking compilation state.
     * @return [Resource.Success] with [OptimizationAnalysis] when analysis completes,
     *         [Resource.Error] otherwise.
     */
    suspend operator fun invoke(mode: AppOptimizationType): Resource<OptimizationAnalysis> {
        return repository.analyzeOptimizationStatus(mode)
    }
}
