package com.tony.appbooster.domain.usecase.analysis
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.scheduler.AnalysisWorkScheduler

/**
 * Enqueues the foreground analysis WorkManager job.
 *
 * Business purpose:
 * - Centralizes work scheduling behind a testable abstraction.
 * - Keeps ViewModels and orchestration code free of WorkManager details.
 *
 * @property scheduler Scheduler responsible for enqueuing and canceling analysis work.
 */
class StartAnalysisWorkUseCase(
    private val scheduler: AnalysisWorkScheduler
) {

    /**
     * Enqueues analysis as unique work.
     *
     * @param mode Optimization mode used for analysis criteria.
     * @return [Resource.Success] when the request is enqueued.
     */
    operator fun invoke(mode: AppOptimizationType): Resource<Unit> {
        scheduler.enqueue(mode)
        return Resource.Success(Unit)
    }
}
