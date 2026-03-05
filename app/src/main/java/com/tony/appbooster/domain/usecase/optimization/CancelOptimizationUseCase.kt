package com.tony.appbooster.domain.usecase.optimization
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AdbRepository

/**
 * Cancels the currently running optimization process.
 *
 * @param repository The repository managing the optimization.
 */
class CancelOptimizationUseCase(
    private val repository: AdbRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return repository.cancelOptimization()
    }
}
