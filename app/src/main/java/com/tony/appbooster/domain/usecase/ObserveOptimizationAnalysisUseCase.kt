package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.repository.AdbRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the current optimization analysis state as a stream.
 *
 * Business purpose:
 * - Keeps analysis observation in the domain layer.
 * - Allows multiple presentation entry points (Dashboard, notification, etc.)
 *   to observe analysis without coupling to the repository.
 *
 * @property repository Repository providing optimization analysis state.
 */
class ObserveOptimizationAnalysisUseCase @Inject constructor(
    private val repository: AdbRepository
) {

    /**
     * @return Hot [StateFlow] emitting the latest [OptimizationAnalysis].
     */
    operator fun invoke(): StateFlow<OptimizationAnalysis> = repository.optimizationAnalysis
}
