package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AdbRepository
import javax.inject.Inject

/**
 * Clears the latest optimization result snapshot.
 *
 * Business purpose:
 * - Makes the "Dismiss" action a real domain event so all UI surfaces become coherent.
 * - Prevents stale progress counters from lingering once the user dismisses the result card.
 *
 * @property repository Repository that owns the optimization progress/result state.
 * @constructor Creates the use case with required repository dependency.
 */
class DismissOptimizationResultUseCase @Inject constructor(
    private val repository: AdbRepository
) {

    /**
     * Clears the latest optimization result and resets the progress counters.
     *
     * @return [Resource.Success] when the snapshot is cleared,
     *         or [Resource.Error] when the repository fails to clear it.
     * @see AdbRepository.clearOptimizationResult
     */
    suspend operator fun invoke(): Resource<Unit> = repository.clearOptimizationResult()
}
