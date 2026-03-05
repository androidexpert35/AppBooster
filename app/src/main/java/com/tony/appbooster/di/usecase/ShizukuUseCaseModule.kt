package com.tony.appbooster.di.usecase

import com.tony.appbooster.domain.client.ShizukuShellClient
import com.tony.appbooster.domain.usecase.shizuku.ObserveShizukuStateUseCase
import com.tony.appbooster.domain.usecase.shizuku.OpenShizukuAppUseCase
import com.tony.appbooster.domain.usecase.shizuku.OpenShizukuInstallPageUseCase
import com.tony.appbooster.domain.usecase.shizuku.RefreshShizukuStateUseCase
import com.tony.appbooster.domain.usecase.shizuku.RequestShizukuPermissionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides Shizuku setup use cases. */
@Module
@InstallIn(SingletonComponent::class)
object ShizukuUseCaseModule {

    @Provides
    @Singleton
    fun provideObserveShizukuStateUseCase(shizukuClient: ShizukuShellClient): ObserveShizukuStateUseCase =
        ObserveShizukuStateUseCase(shizukuClient)

    @Provides
    @Singleton
    fun provideRefreshShizukuStateUseCase(shizukuClient: ShizukuShellClient): RefreshShizukuStateUseCase =
        RefreshShizukuStateUseCase(shizukuClient)

    @Provides
    @Singleton
    fun provideRequestShizukuPermissionUseCase(shizukuClient: ShizukuShellClient): RequestShizukuPermissionUseCase =
        RequestShizukuPermissionUseCase(shizukuClient)

    @Provides
    @Singleton
    fun provideOpenShizukuInstallPageUseCase(shizukuClient: ShizukuShellClient): OpenShizukuInstallPageUseCase =
        OpenShizukuInstallPageUseCase(shizukuClient)

    @Provides
    @Singleton
    fun provideOpenShizukuAppUseCase(shizukuClient: ShizukuShellClient): OpenShizukuAppUseCase =
        OpenShizukuAppUseCase(shizukuClient)
}

