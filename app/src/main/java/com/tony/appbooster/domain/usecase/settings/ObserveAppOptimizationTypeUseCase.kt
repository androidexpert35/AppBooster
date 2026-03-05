package com.tony.appbooster.domain.usecase.settings
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case responsible for observing the current optimization behavior
 * so that presentation layer can reactively update the UI.
 *
 * @param repository The [SettingsRepository] providing access to persisted configuration.
 */
class ObserveAppOptimizationTypeUseCase(
    private val repository: SettingsRepository
) {

    /**
     * Executes the observation of the optimization setting from the repository,
     * exposing a [Flow] so the caller can collect and update UI state.
     *
     * @return A [Flow] emitting [Resource] instances for [AppOptimizationType] changes.
     */
    operator fun invoke(): Flow<Resource<AppOptimizationType>> {
        return repository.observeAppOptimizationType()
    }
}
