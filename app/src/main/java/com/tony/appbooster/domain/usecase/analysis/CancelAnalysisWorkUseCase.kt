package com.tony.appbooster.domain.usecase.analysis
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.scheduler.AnalysisWorkScheduler

/**
 * Cancels any running analysis work.
 *
 * Business purpose:
 * - Keeps WorkManager cancellation details out of ViewModels.
 * - Allows stop to be triggered consistently.
 *
 * @property scheduler Scheduler responsible for canceling analysis work.
 */
class CancelAnalysisWorkUseCase(
    private val scheduler: AnalysisWorkScheduler
) {

    /**
     * @return [Resource.Success] after issuing cancellation.
     */
    operator fun invoke(): Resource<Unit> {
        scheduler.cancel()
        return Resource.Success(Unit)
    }
}
