package com.tony.appbooster.presentation.error

import android.content.Context
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.common.ResourceError

/**
 * Maps domain-layer [ResourceError] instances to user-facing UI messages.
 *
 * Business purpose:
 * - Ensures error rendering stays consistent across screens and entry points.
 * - Avoids leaking domain error model details into ViewModels and Composables.
 */
object ResourceErrorUiMapper {

    /**
     * Converts a [ResourceError] into a localized, user-friendly message.
     *
     * @param context Android context used to resolve string resources.
     * @param error Domain error model.
     * @return Human-readable message suitable for snackbars/dialogs.
     */
    fun toUserMessage(context: Context, error: ResourceError): String {
        return when (error) {
            is ResourceError.LogicError -> error.errorMessage
                ?: context.getString(R.string.error_generic_fallback_message)

            is ResourceError.NetworkError -> error.errorMessage
                ?: context.getString(R.string.error_network_message)

            is ResourceError.DatabaseError -> error.message

            is ResourceError.SSLError -> context.getString(R.string.error_security_message)

            is ResourceError.UnknownError -> context.getString(R.string.error_unknown_fallback_message)
        }
    }
}
