package com.tony.appbooster.domain.model.common

/**
 * Represents static application metadata used for displaying build information
 * to the user within the Settings or About screens.
 *
 * @property versionName Human-readable application version name (e.g. "1.0.0").
 * @property buildChannel Optional build channel label (e.g. "Alpha", "Beta", "Release").
 */
data class AppInfo(
    val versionName: String,
    val buildChannel: String?
)

