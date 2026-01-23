package com.tony.appbooster.di

import com.tony.appbooster.data.client.WearableDataClientImpl
import com.tony.appbooster.domain.client.WearableDataClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Wearable Data Layer bindings.
 *
 * Provides dependencies for phone-watch communication via Google Play Services Wearable API.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WearableModule {

    /**
     * Binds the Wearable Data Layer client implementation.
     *
     * @param impl Implementation using Google Play Services Wearable API.
     * @return Bound [WearableDataClient] abstraction.
     */
    @Binds
    @Singleton
    abstract fun bindWearableDataClient(
        impl: WearableDataClientImpl
    ): WearableDataClient
}
