package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AdbRepository
import javax.inject.Inject

/**
 * Ensures that the app has an active shell connection (Shizuku / ADB bridge).
 *
 * Business purpose:
 * - Centralizes connection preconditions so both analysis and optimization
 *   can reuse a single, testable dependency.
 *
 * @property repository Repository responsible for establishing and validating connectivity.
 */
class EnsureAdbConnectedUseCase @Inject constructor(
    private val repository: AdbRepository
) {

    /**
     * Ensures an active connection.
     *
     * @return [Resource.Success] when connected, [Resource.Error] otherwise.
     */
    suspend operator fun invoke(): Resource<Unit> = repository.ensureConnected()
}
