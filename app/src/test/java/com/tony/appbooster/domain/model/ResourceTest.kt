package com.tony.appbooster.domain.model

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [Resource] sealed class and its [ResourceError] subtypes.
 *
 * Verifies construction, pattern matching, and equality semantics across all variants.
 */
class ResourceTest {

    // ── Resource.Success ─────────────────────────────────────────────────────

    @Test
    fun `given success data when wrapped in Resource Success then data is accessible`() {
        val resource: Resource<String> = Resource.Success("hello")
        assertTrue(resource is Resource.Success)
        assertEquals("hello", (resource as Resource.Success).data)
    }

    @Test
    fun `given two Success with same data when compared then equal`() {
        assertEquals(Resource.Success(42), Resource.Success(42))
    }

    // ── Resource.Error ────────────────────────────────────────────────────────

    @Test
    fun `given LogicError when wrapped in Resource Error then error data accessible`() {
        val error = ResourceError.LogicError("bad logic", errorCode = "E001")
        val resource: Resource<Nothing> = Resource.Error(error)
        assertTrue(resource is Resource.Error)
        assertEquals(error, (resource as Resource.Error).data)
    }

    @Test
    fun `given NetworkError when wrapped in Resource Error then error data accessible`() {
        val error = ResourceError.NetworkError("timeout")
        val resource = Resource.Error(error)
        assertEquals(error, (resource as Resource.Error).data)
    }

    @Test
    fun `given DatabaseError when wrapped in Resource Error then message preserved`() {
        val error = ResourceError.DatabaseError("disk full")
        val resource = Resource.Error(error)
        assertEquals("disk full", ((resource as Resource.Error).data as ResourceError.DatabaseError).message)
    }

    @Test
    fun `given UnknownError when wrapped in Resource Error then is UnknownError`() {
        val resource = Resource.Error(ResourceError.UnknownError)
        assertTrue((resource as Resource.Error).data is ResourceError.UnknownError)
    }

    // ── ResourceError equality ────────────────────────────────────────────────

    @Test
    fun `given two LogicError with same fields when compared then equal`() {
        assertEquals(
            ResourceError.LogicError("msg", "code"),
            ResourceError.LogicError("msg", "code")
        )
    }

    @Test
    fun `given two NetworkError with same message when compared then equal`() {
        assertEquals(
            ResourceError.NetworkError("error"),
            ResourceError.NetworkError("error")
        )
    }

    @Test
    fun `given LogicError with null message when accessing message then null`() {
        val error = ResourceError.LogicError(errorMessage = null)
        assertEquals(null, error.errorMessage)
    }
}

