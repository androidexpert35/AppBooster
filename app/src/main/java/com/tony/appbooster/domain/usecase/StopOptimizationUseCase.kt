package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.Resource
import javax.inject.Inject

/**
 * Stops an active optimization run.
 *
 * Business purpose:
 * - Ensures all entry points (UI, notification action, future automation) stop the operation
 *   using the exact same sequence.
 * - Prevents drift where one path cancels WorkManager but forgets repository-side cancellation.
 *
 * Stop semantics:
 * 1) Cancel scheduled foreground work (removes notification / stops Worker).
 * 2) Cancel repository-side in-flight operations and update UI state immediately.
 *
 * @property cancelOptimizationWorkUseCase Cancels WorkManager-backed optimization work.
 * @property cancelOptimizationUseCase Cancels repository-side optimization loop.
 */
class StopOptimizationUseCase @Inject constructor(
    private val cancelOptimizationWorkUseCase: CancelOptimizationWorkUseCase,
    private val cancelOptimizationUseCase: CancelOptimizationUseCase
) {

    /**
     * Requests the stop operation.
     *
     * @return [Resource.Success] when stop was requested.
     */
    suspend operator fun invoke(): Resource<Unit> {
        cancelOptimizationWorkUseCase()
        return cancelOptimizationUseCase()
    }
}
