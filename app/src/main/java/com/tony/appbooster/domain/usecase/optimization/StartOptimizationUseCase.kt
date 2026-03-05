package com.tony.appbooster.domain.usecase.optimization

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.usecase.adb.ConnectAdbUseCase

/**
 * Starts an optimization run by ensuring connectivity and enqueuing foreground work.
 *
 * Business purpose:
 * - Provides a single entry point for starting optimization.
 * - Centralizes connectivity preconditions and scheduling.
 *
 * @property connectAdbUseCase Use case that establishes ADB/Shizuku connectivity.
 * @property startOptimizationWorkUseCase Use case that schedules optimization work.
 */
class StartOptimizationUseCase(
    private val connectAdbUseCase: ConnectAdbUseCase,
    private val startOptimizationWorkUseCase: StartOptimizationWorkUseCase
) {

    /**
     * Starts optimization for the given mode.
     *
     * @param mode Optimization mode to execute.
     * @return [Resource.Success] when work is scheduled, [Resource.Error] otherwise.
     */
    suspend operator fun invoke(mode: AppOptimizationType): Resource<Unit> {
        return when (val connection = connectAdbUseCase()) {
            is Resource.Success -> startOptimizationWorkUseCase(mode)
            is Resource.Error -> connection
        }
    }
}
