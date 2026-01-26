package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AdbRepository
import javax.inject.Inject

/**
 * Cancels the currently running analysis process.
 *
 * @param repository The repository managing the analysis.
 */
class CancelAnalysisUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return repository.cancelAnalysis()
    }
}
