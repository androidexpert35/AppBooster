package com.tony.appbooster.domain.usecase.adb
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AdbRepository

/**
 * Ensures the Shizuku-based shell connection is ready for use.
 *
 * With Shizuku, there's no manual pairing or connection - we just verify
 * that Shizuku is running and permission is granted.
 *
 * @param repository Repository providing ADB/shell connection operations.
 */
class ConnectAdbUseCase(
    private val repository: AdbRepository
) {

    /**
     * Ensures the shell connection is ready.
     *
     * @return [Resource.Success] when ready, [Resource.Error] otherwise.
     */
    suspend operator fun invoke(): Resource<Unit> {
        return repository.ensureConnected()
    }
}
