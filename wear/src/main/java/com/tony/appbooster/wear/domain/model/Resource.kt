package com.tony.appbooster.wear.domain.model

/**
 * A sealed class representing the state of a data operation.
 * It can be either Success, holding the data, or Error, holding a ResourceError.
 *
 * @param T The type of the data in case of success.
 */
sealed class Resource<out T> {
    /**
     * Represents a successful operation.
     *
     * @param data The data retrieved or processed.
     */
    data class Success<T>(val data: T) : Resource<T>()

    /**
     * Represents a failed operation.
     *
     * @param error The details of the error.
     */
    data class Error(val error: ResourceError) : Resource<Nothing>()
}

/**
 * A sealed class representing different types of errors that can occur during an operation.
 */
sealed class ResourceError {
    /**
     * Represents an error in the application logic or data processing.
     *
     * @param errorMessage A descriptive message for the error.
     * @param errorCode An optional code identifying the specific error.
     */
    data class LogicError(
        val errorMessage: String?,
        val errorCode: String? = null
    ) : ResourceError()

    /**
     * Represents an ADB connection or communication error.
     *
     * @param errorMessage A descriptive message for the error.
     * @param exception The underlying exception that caused the error.
     */
    data class AdbError(
        val errorMessage: String?,
        val exception: Throwable? = null
    ) : ResourceError()
}
