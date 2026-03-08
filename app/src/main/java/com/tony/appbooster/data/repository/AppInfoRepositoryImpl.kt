package com.tony.appbooster.data.repository


import android.content.Context
import android.content.pm.PackageManager
import com.tony.appbooster.domain.model.common.AppInfo
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.repository.AppInfoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [AppInfoRepository] that reads application
 * metadata from the Android [PackageManager] and exposes it as a domain-level
 * [Resource] for consumption by the use case and presentation layers.
 *
 * This repository isolates Android framework dependencies from the domain
 * while providing the current version name and code for display in Settings
 * or about screens.
 *
 * @property applicationContext Application-level [Context] used to access
 *           Android package metadata.
 */
@Singleton
class AppInfoRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context
) : AppInfoRepository {

    /**
     * Loads metadata for the currently installed application build, including
     * version name and version code, and maps any framework exceptions to a
     * domain-specific [ResourceError].
     *
     * @return [Resource.Success] containing [AppInfo] when the package info is
     * resolved, or [Resource.Error] with [ResourceError.LogicError] when the
     * data cannot be obtained.
     */
    override suspend fun getAppInfo(): Resource<AppInfo> {
        return try {
            val packageManager = applicationContext.packageManager
            val packageName = applicationContext.packageName

            val packageInfo = packageManager.getPackageInfo(packageName, 0)

            val versionName = packageInfo.versionName ?: "0.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }

            // In case you have additional UI-model fields (e.g. channel), set them here.
            val appInfo = AppInfo(
                versionName = versionName,
                buildChannel = versionCode
            )

            Resource.Success(appInfo)
        } catch (throwable: Throwable) {
            Resource.Error(
                ResourceError.LogicError(
                    errorMessage = throwable.message ?: "Unable to load application info",
                    errorCode = "APP_INFO_LOAD_FAILURE"
                )
            )
        }
    }
}
