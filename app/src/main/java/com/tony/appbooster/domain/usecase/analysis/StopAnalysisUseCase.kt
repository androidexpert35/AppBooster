package com.tony.appbooster.domain.usecase.analysis
import com.tony.appbooster.domain.model.common.Resource

/**
 * Stops an active analysis run.
 *
 * Business purpose:
 * - Ensures analysis can be stopped consistently from any entry point.
 * - Cancels both background work and repository-side state promptly.
 *
 * Stop semantics:
 * 1) Cancel scheduled foreground work (removes notification / stops Worker).
 * 2) Cancel repository-side analysis loop and update UI state immediately.
 *
 * @property cancelAnalysisWorkUseCase Cancels WorkManager-backed analysis work.
 * @property cancelAnalysisUseCase Cancels repository-side analysis loop.
 */
class StopAnalysisUseCase(
    private val cancelAnalysisWorkUseCase: CancelAnalysisWorkUseCase,
    private val cancelAnalysisUseCase: CancelAnalysisUseCase
) {

    /**
     * Requests the stop operation.
     *
     * @return [Resource.Success] when stop was requested.
     */
    suspend operator fun invoke(): Resource<Unit> {
        cancelAnalysisWorkUseCase()
        return cancelAnalysisUseCase()
    }
}
