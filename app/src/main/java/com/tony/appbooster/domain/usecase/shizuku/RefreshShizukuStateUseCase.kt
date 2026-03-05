package com.tony.appbooster.domain.usecase.shizuku

import com.tony.appbooster.domain.client.ShizukuShellClient

/**
 * Refreshes the current Shizuku state by checking service availability.
 *
 * @property shizukuClient Client responsible for Shizuku interactions.
 */
class RefreshShizukuStateUseCase(
    private val shizukuClient: ShizukuShellClient
) {
    /**
     * Requests a state refresh from the Shizuku client.
     */
    suspend operator fun invoke() = shizukuClient.refreshState()
}

