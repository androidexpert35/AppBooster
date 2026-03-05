package com.tony.appbooster.domain.usecase.shizuku

import com.tony.appbooster.domain.client.ShizukuShellClient
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import kotlinx.coroutines.flow.StateFlow

/**
 * Observes the current Shizuku service state for UI updates.
 *
 * @property shizukuClient Client exposing Shizuku state stream.
 */
class ObserveShizukuStateUseCase(
    private val shizukuClient: ShizukuShellClient
) {
    /**
     * Returns a stream that emits Shizuku state changes.
     *
     * @return [StateFlow] of [ShizukuState].
     */
    operator fun invoke(): StateFlow<ShizukuState> = shizukuClient.state
}

