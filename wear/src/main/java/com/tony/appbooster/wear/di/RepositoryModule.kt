package com.tony.appbooster.wear.di

import com.tony.appbooster.wear.data.repository.WearAdbRepositoryImpl
import com.tony.appbooster.wear.domain.repository.WearAdbRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository bindings for the Wear OS app.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the [WearAdbRepositoryImpl] to its interface.
     */
    @Binds
    @Singleton
    abstract fun bindWearAdbRepository(
        impl: WearAdbRepositoryImpl
    ): WearAdbRepository
}
