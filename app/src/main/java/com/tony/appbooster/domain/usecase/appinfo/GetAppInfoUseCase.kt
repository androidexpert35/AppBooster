package com.tony.appbooster.domain.usecase.appinfo
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.repository.AppInfoRepository
import com.tony.appbooster.presentation.screen.settings.model.AppInfo

/**
 * Use case responsible for retrieving app metadata required by the Settings
 * screen, delegating to the [AppInfoRepository] and preserving typed errors.
 *
 * @param appInfoRepository Repository providing app version and channel data.
 * @return [Resource]\<[AppInfo]\> that can be mapped directly to UI state.
 */
class GetAppInfoUseCase(
    private val appInfoRepository: AppInfoRepository
) {

    /**
     * Executes the app info loading flow to obtain the current build metadata
     * for display in Settings and other informational screens.
     *
     * @return [Resource.Success] containing [AppInfo] on success, or
     * [Resource.Error] describing the failure cause.
     */
    suspend operator fun invoke(): Resource<AppInfo> {
        return appInfoRepository.getAppInfo()
    }
}

