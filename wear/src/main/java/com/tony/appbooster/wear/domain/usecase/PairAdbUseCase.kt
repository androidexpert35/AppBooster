package com.tony.appbooster.wear.domain.usecase

import com.tony.appbooster.wear.domain.model.Resource
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import javax.inject.Inject

/**
 * Pairs with the local ADB daemon using wireless debugging credentials.
 *
 * This is typically a one-time operation. After successful pairing,
 * the RSA key is trusted and future connections don't require pairing.
 *
 * @property repository Repository providing ADB operations.
 */
class PairAdbUseCase @Inject constructor(
    private val repository: WearAdbRepository
) {
    /**
     * Executes the pairing flow with the given credentials.
     *
     * @param port The pairing port from Wireless Debugging settings.
     * @param pairingCode The 6-digit pairing code from settings.
     * @return [Resource.Success] if pairing succeeded, [Resource.Error] otherwise.
     */
    suspend operator fun invoke(port: Int, pairingCode: String): Resource<Unit> {
        return repository.pair(port, pairingCode)
    }
}
