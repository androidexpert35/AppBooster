package com.tony.appbooster.domain.usecase.optimization
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbRepository

/**
 * Triggers ART optimization for all installed applications using the
 * configured compile mode on the active ADB session.
 *
 * @param repository Repository used to send optimization commands over ADB.
 * @return [Resource.Success] when optimization completes, [Resource.Error] otherwise.
 */
class OptimizeAppUseCase(
    private val repository: AdbRepository
) {

    /**
     * Executes the optimization command with the given mode.
     *
     * @param mode ART compile mode used for package optimization.
     * @return [Resource] describing the optimization outcome.
     */
    suspend operator fun invoke(
        mode: AppOptimizationType
    ): Resource<Unit> = repository.executeOptimizationCommand(mode)
}
