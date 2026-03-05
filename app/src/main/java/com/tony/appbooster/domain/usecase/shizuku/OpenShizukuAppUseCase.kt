package com.tony.appbooster.domain.usecase.shizuku

import com.tony.appbooster.domain.client.ShizukuShellClient

/**
 * Opens the Shizuku app so the user can start the service.
 *
 * @property shizukuClient Client that can launch the Shizuku app.
 */
class OpenShizukuAppUseCase(
    private val shizukuClient: ShizukuShellClient
) {
    /**
     * Launches the Shizuku application.
     */
    operator fun invoke() = shizukuClient.openShizukuApp()
}

