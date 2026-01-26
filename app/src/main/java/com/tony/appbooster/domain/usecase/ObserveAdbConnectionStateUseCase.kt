package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.repository.AdbConnectionState
import com.tony.appbooster.domain.repository.AdbRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the current ADB connection state as a stream.
 *
 * Business purpose:
 * - Prevents presentation layer from depending directly on repository flows.
 * - Keeps the ViewModel API consistent (use-cases only).
 *
 * @property repository Repository providing ADB connection signals.
 */
class ObserveAdbConnectionStateUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    /**
     * @return Hot [StateFlow] emitting the latest [AdbConnectionState].
     */
    operator fun invoke(): StateFlow<AdbConnectionState> = repository.connectionState
}
