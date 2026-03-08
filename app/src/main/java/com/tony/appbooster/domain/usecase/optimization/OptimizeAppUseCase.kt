package com.tony.appbooster.domain.usecase.optimization
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbRepository

/**
 * Triggers ART optimization for all installed applications using the
 * configured compile mode on the active ADB session.
 *
 * @property repository Repository used to send optimization commands over ADB.
 */
class OptimizeAppUseCase(
    private val repository: AdbRepository
) {

    /**
     * Executes the optimization command with the given mode.
     *
     * @param mode ART compile mode used for package optimization.
     * @param forceOptimize When true, compiles every installed package
     *        regardless of its current compilation status. Useful after
     *        OTA updates when apps revert to "verify".
     * @return [Resource] describing the optimization outcome.
     */
    suspend operator fun invoke(
        mode: AppOptimizationType,
        forceOptimize: Boolean = false
    ): Resource<Unit> = repository.executeOptimizationCommand(mode, forceOptimize)
}
