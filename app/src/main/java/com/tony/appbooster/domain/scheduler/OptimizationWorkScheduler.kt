package com.tony.appbooster.domain.scheduler

import com.tony.appbooster.domain.model.settings.AppOptimizationType

/**
 * Schedules and cancels foreground optimization work.
 *
 * Business purpose:
 * - Keeps WorkManager/API details out of ViewModels and (optionally) out of domain use-cases.
 * - Allows triggering optimization uniformly from UI, notifications, and future entry points.
 */
interface OptimizationWorkScheduler {

    /**
     * Enqueues a unique optimization run.
     *
     * @param mode Optimization mode to execute.
     * @param forceOptimize When true, compiles every installed package
     *        regardless of its current compilation status.
     */
    fun enqueue(mode: AppOptimizationType, forceOptimize: Boolean = false)

    /**
     * Cancels the currently running optimization work, if any.
     */
    fun cancel()
}
