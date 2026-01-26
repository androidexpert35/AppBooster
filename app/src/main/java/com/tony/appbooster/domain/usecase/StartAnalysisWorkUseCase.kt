package com.tony.appbooster.domain.usecase

import android.content.Context
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.presentation.worker.AnalysisWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Starts the foreground analysis WorkManager job.
 *
 * Business purpose:
 * - Centralizes WorkManager enqueue logic so presentation can trigger analysis
 *   via a single use-case.
 * - Makes it easy to reuse the exact same behavior from multiple entry points
 *   (Analyze button, optimization prerequisite, etc.).
 *
 * Note:
 * - This use-case currently lives in the domain package for convenience, but
 *   it depends on Android and WorkManager. If you want strict purity, we can
 *   move this to a data-layer scheduler behind a domain interface.
 *
 * @property appContext Application context used for enqueuing work.
 */
class StartAnalysisWorkUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context
) {

    /**
     * Enqueues analysis as unique work.
     *
     * @param mode Optimization mode used for analysis criteria.
     * @return [Resource.Success] when the request is enqueued.
     */
    operator fun invoke(mode: AppOptimizationType): Resource<Unit> {
        AnalysisWorker.enqueue(appContext, mode)
        return Resource.Success(Unit)
    }
}
