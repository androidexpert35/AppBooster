package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.OptimizationLogEntry
import com.tony.appbooster.domain.repository.AdbRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the structured activity log entries as a stream.
 *
 * Business purpose:
 * - Keeps ViewModels independent of repository implementation details.
 * - Enables consistent activity feed across screens.
 *
 * @property repository Repository providing log entry stream.
 */
class ObserveOptimizationLogEntriesUseCase @Inject constructor(
    private val repository: AdbRepository
) {

    /**
     * @return Hot [StateFlow] emitting the latest list of [OptimizationLogEntry].
     */
    operator fun invoke(): StateFlow<List<OptimizationLogEntry>> = repository.logEntries
}
