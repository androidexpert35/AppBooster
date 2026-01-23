package com.tony.appbooster.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Wear OS Application class with Hilt dependency injection.
 *
 * This is the entry point for the Wear OS app, initializing Hilt
 * for dependency injection across all components.
 */
@HiltAndroidApp
class WearApp : Application()
