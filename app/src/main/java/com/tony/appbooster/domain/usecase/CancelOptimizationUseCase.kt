package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AdbRepository
import javax.inject.Inject

/**
 * Cancels the currently running optimization process.
 *
 * @param repository The repository managing the optimization.
 */
class CancelOptimizationUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return repository.cancelOptimization()
    }
}
