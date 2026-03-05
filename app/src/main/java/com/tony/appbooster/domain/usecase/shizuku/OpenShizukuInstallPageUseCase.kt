package com.tony.appbooster.domain.usecase.shizuku

import com.tony.appbooster.domain.client.ShizukuShellClient

/**
 * Opens the Shizuku installation page for first-time setup.
 *
 * @property shizukuClient Client that can open external Shizuku destinations.
 */
class OpenShizukuInstallPageUseCase(
    private val shizukuClient: ShizukuShellClient
) {
    /**
     * Opens the install page in browser or Play Store.
     */
    operator fun invoke() = shizukuClient.openShizukuInstallPage()
}

