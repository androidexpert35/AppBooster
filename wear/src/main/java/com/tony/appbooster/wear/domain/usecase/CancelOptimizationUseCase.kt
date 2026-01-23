package com.tony.appbooster.wear.domain.usecase

import com.tony.appbooster.wear.domain.model.Resource
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import javax.inject.Inject

/**
 * Cancels the currently running optimization workflow.
 *
 * @property repository Repository providing ADB and optimization operations.
 */
class CancelOptimizationUseCase @Inject constructor(
    private val repository: WearAdbRepository
) {
    /**
     * Requests cancellation of the ongoing optimization.
     *
     * @return [Resource.Success] if cancellation was requested successfully.
     */
    suspend operator fun invoke(): Resource<Unit> {
        return repository.cancelOptimization()
    }
}
