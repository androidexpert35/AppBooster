package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.repository.AdbRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the current optimization progress as a stream.
 *
 * Business purpose:
 * - Keeps progress observation in the domain layer.
 * - Allows ViewModels to depend on use-cases only.
 *
 * @property repository Repository providing optimization progress updates.
 */
class ObserveOptimizationProgressUseCase @Inject constructor(
    private val repository: AdbRepository
) {

    /**
     * @return Hot [StateFlow] emitting the latest [OptimizationProgress].
     */
    operator fun invoke(): StateFlow<OptimizationProgress> = repository.optimizationProgress
}
