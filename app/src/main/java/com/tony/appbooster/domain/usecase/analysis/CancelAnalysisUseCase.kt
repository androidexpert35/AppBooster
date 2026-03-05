package com.tony.appbooster.domain.usecase.analysis
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AdbRepository

/**
 * Cancels the currently running analysis process.
 *
 * @param repository The repository managing the analysis.
 */
class CancelAnalysisUseCase(
    private val repository: AdbRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return repository.cancelAnalysis()
    }
}
