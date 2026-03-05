package com.tony.appbooster.di.usecase

import com.tony.appbooster.domain.repository.AdbRepository
import com.tony.appbooster.domain.scheduler.OptimizationWorkScheduler
import com.tony.appbooster.domain.usecase.adb.ConnectAdbUseCase
import com.tony.appbooster.domain.usecase.optimization.CancelOptimizationUseCase
import com.tony.appbooster.domain.usecase.optimization.CancelOptimizationWorkUseCase
import com.tony.appbooster.domain.usecase.optimization.DismissOptimizationResultUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveCommandOutputUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveOptimizationLogEntriesUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveOptimizationProgressUseCase
import com.tony.appbooster.domain.usecase.optimization.OptimizeAppUseCase
import com.tony.appbooster.domain.usecase.optimization.StartOptimizationUseCase
import com.tony.appbooster.domain.usecase.optimization.StartOptimizationWorkUseCase
import com.tony.appbooster.domain.usecase.optimization.StopOptimizationUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides optimization-related use cases. */
@Module
@InstallIn(SingletonComponent::class)
object OptimizationUseCaseModule {

    @Provides
    @Singleton
    fun provideCancelOptimizationUseCase(adbRepository: AdbRepository): CancelOptimizationUseCase =
        CancelOptimizationUseCase(adbRepository)

    @Provides
    @Singleton
    fun provideCancelOptimizationWorkUseCase(scheduler: OptimizationWorkScheduler): CancelOptimizationWorkUseCase =
        CancelOptimizationWorkUseCase(scheduler)

    @Provides
    @Singleton
    fun provideDismissOptimizationResultUseCase(adbRepository: AdbRepository): DismissOptimizationResultUseCase =
        DismissOptimizationResultUseCase(adbRepository)

    @Provides
    @Singleton
    fun provideObserveCommandOutputUseCase(adbRepository: AdbRepository): ObserveCommandOutputUseCase =
        ObserveCommandOutputUseCase(adbRepository)

    @Provides
    @Singleton
    fun provideObserveOptimizationLogEntriesUseCase(adbRepository: AdbRepository): ObserveOptimizationLogEntriesUseCase =
        ObserveOptimizationLogEntriesUseCase(adbRepository)

    @Provides
    @Singleton
    fun provideObserveOptimizationProgressUseCase(adbRepository: AdbRepository): ObserveOptimizationProgressUseCase =
        ObserveOptimizationProgressUseCase(adbRepository)

    @Provides
    @Singleton
    fun provideOptimizeAppUseCase(adbRepository: AdbRepository): OptimizeAppUseCase =
        OptimizeAppUseCase(adbRepository)

    @Provides
    @Singleton
    fun provideStartOptimizationWorkUseCase(scheduler: OptimizationWorkScheduler): StartOptimizationWorkUseCase =
        StartOptimizationWorkUseCase(scheduler)

    @Provides
    @Singleton
    fun provideStartOptimizationUseCase(
        connectAdbUseCase: ConnectAdbUseCase,
        startOptimizationWorkUseCase: StartOptimizationWorkUseCase
    ): StartOptimizationUseCase = StartOptimizationUseCase(connectAdbUseCase, startOptimizationWorkUseCase)

    @Provides
    @Singleton
    fun provideStopOptimizationUseCase(
        cancelOptimizationWorkUseCase: CancelOptimizationWorkUseCase,
        cancelOptimizationUseCase: CancelOptimizationUseCase
    ): StopOptimizationUseCase =
        StopOptimizationUseCase(cancelOptimizationWorkUseCase, cancelOptimizationUseCase)
}

