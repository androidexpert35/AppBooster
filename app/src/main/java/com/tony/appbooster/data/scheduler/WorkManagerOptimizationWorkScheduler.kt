package com.tony.appbooster.data.scheduler

import android.content.Context
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.scheduler.OptimizationWorkScheduler
import com.tony.appbooster.presentation.worker.OptimizationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * WorkManager-backed implementation of [OptimizationWorkScheduler].
 *
 * Business purpose:
 * - Encapsulates WorkManager API calls in the data layer.
 * - Keeps ViewModels and domain orchestration code free from WorkManager details.
 */
class WorkManagerOptimizationWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) : OptimizationWorkScheduler {

    override fun enqueue(mode: AppOptimizationType, forceOptimize: Boolean) {
        OptimizationWorker.enqueue(context, mode, forceOptimize)
    }

    override fun cancel() {
        OptimizationWorker.cancel(context)
    }
}
