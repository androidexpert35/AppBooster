package com.tony.appbooster.di.usecase

import com.tony.appbooster.domain.repository.AppInfoRepository
import com.tony.appbooster.domain.usecase.appinfo.GetAppInfoUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides app-info related use cases. */
@Module
@InstallIn(SingletonComponent::class)
object AppInfoUseCaseModule {

    @Provides
    @Singleton
    fun provideGetAppInfoUseCase(appInfoRepository: AppInfoRepository): GetAppInfoUseCase =
        GetAppInfoUseCase(appInfoRepository)
}

