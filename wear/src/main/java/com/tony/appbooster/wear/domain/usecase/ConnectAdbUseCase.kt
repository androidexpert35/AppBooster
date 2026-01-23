package com.tony.appbooster.wear.domain.usecase

import com.tony.appbooster.wear.domain.model.Resource
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import javax.inject.Inject

/**
 * Connects to the local ADB daemon for shell command execution.
 *
 * Requires wireless debugging to be enabled and prior successful pairing.
 *
 * @property repository Repository providing ADB operations.
 */
class ConnectAdbUseCase @Inject constructor(
    private val repository: WearAdbRepository
) {
    /**
     * Attempts to connect to the ADB daemon on the specified port.
     *
     * @param port The connection port from Wireless Debugging settings.
     * @return [Resource.Success] if connected, [Resource.Error] otherwise.
     */
    suspend operator fun invoke(port: Int): Resource<Unit> {
        return repository.connect(port)
    }

    /**
     * Attempts auto-discovery and connection.
     *
     * @return [Resource.Success] if connected, [Resource.Error] otherwise.
     */
    suspend fun autoConnect(): Resource<Unit> {
        return repository.autoConnect()
    }
}
