package com.tony.appbooster.wear.domain.usecase

import com.tony.appbooster.wear.domain.model.OptimizationType
import com.tony.appbooster.wear.domain.model.Resource
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import javax.inject.Inject

/**
 * Triggers ART optimization for all installed applications on the watch.
 *
 * Uses the ADB connection to execute package compilation commands
 * with the specified optimization mode.
 *
 * @property repository Repository providing ADB and optimization operations.
 */
class OptimizeAppsUseCase @Inject constructor(
    private val repository: WearAdbRepository
) {
    /**
     * Executes the optimization workflow for all installed packages.
     *
     * @param mode The optimization mode to use.
     * @return [Resource.Success] when complete, [Resource.Error] on failure.
     */
    suspend operator fun invoke(mode: OptimizationType): Resource<Unit> {
        return repository.executeOptimization(mode)
    }
}
