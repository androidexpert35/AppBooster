package com.tony.appbooster.di.usecase

import com.tony.appbooster.domain.repository.SettingsRepository
import com.tony.appbooster.domain.usecase.settings.ObserveAppOptimizationTypeUseCase
import com.tony.appbooster.domain.usecase.settings.SetAppOptimizationTypeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides settings-related use cases. */
@Module
@InstallIn(SingletonComponent::class)
object SettingsUseCaseModule {

    @Provides
    @Singleton
    fun provideObserveAppOptimizationTypeUseCase(settingsRepository: SettingsRepository): ObserveAppOptimizationTypeUseCase =
        ObserveAppOptimizationTypeUseCase(settingsRepository)

    @Provides
    @Singleton
    fun provideSetAppOptimizationTypeUseCase(settingsRepository: SettingsRepository): SetAppOptimizationTypeUseCase =
        SetAppOptimizationTypeUseCase(settingsRepository)
}

