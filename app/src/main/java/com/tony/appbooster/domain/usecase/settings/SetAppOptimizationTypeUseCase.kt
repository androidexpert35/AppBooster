package com.tony.appbooster.domain.usecase.settings
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.SettingsRepository

/**
 * Use case responsible for persisting the selected optimization behavior,
 * allowing the user to control compilation/runtime trade-offs.
 *
 * @param repository The [SettingsRepository] used to persist configuration changes.
 */
class SetAppOptimizationTypeUseCase(
    private val repository: SettingsRepository
) {

    /**
     * Persists the specified optimization type via the underlying repository,
     * returning a [Resource] so the caller can surface errors to the UI.
     *
     * @param type The selected [AppOptimizationType] to be stored.
     * @return A [Resource] describing success or error of the persistence operation.
     */
    suspend operator fun invoke(type: AppOptimizationType): Resource<Unit> {
        return repository.setAppOptimizationType(type)
    }
}
