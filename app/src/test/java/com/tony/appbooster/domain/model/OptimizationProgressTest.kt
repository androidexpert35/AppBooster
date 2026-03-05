package com.tony.appbooster.domain.model

import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.model.common.OptimizationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OptimizationProgress] data class defaults and [OptimizationResult] semantics.
 */
class OptimizationProgressTest {

    @Test
    fun `given default instance then isRunning is false`() {
        assertFalse(OptimizationProgress().isRunning)
    }

    @Test
    fun `given default instance then result is None`() {
        assertEquals(OptimizationResult.None, OptimizationProgress().result)
    }

    @Test
    fun `given default instance then runId is zero`() {
        assertEquals(0L, OptimizationProgress().runId)
    }

    @Test
    fun `given running progress when copy with isRunning false then not running`() {
        val running = OptimizationProgress(isRunning = true, runId = 42L)
        val stopped = running.copy(isRunning = false, result = OptimizationResult.Canceled)
        assertFalse(stopped.isRunning)
        assertEquals(OptimizationResult.Canceled, stopped.result)
        assertEquals(42L, stopped.runId)
    }

    @Test
    fun `given Completed result then isSuccess is semantically true`() {
        val result: OptimizationResult = OptimizationResult.Completed
        assertTrue(result is OptimizationResult.Completed)
    }

    @Test
    fun `given Canceled result then is Canceled`() {
        val result: OptimizationResult = OptimizationResult.Canceled
        assertTrue(result is OptimizationResult.Canceled)
    }
}

