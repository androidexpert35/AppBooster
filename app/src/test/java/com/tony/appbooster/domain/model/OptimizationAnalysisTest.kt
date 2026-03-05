package com.tony.appbooster.domain.model

import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OptimizationAnalysis] computed properties.
 */
class OptimizationAnalysisTest {

    // --- hasScanned ---

    @Test
    fun `given lastScanTimeMs is null when hasScanned then returns false`() {
        val analysis = OptimizationAnalysis(lastScanTimeMs = null)
        assertFalse(analysis.hasScanned)
    }

    @Test
    fun `given lastScanTimeMs is set when hasScanned then returns true`() {
        val analysis = OptimizationAnalysis(lastScanTimeMs = 1_000L)
        assertTrue(analysis.hasScanned)
    }

    // --- allOptimized ---

    @Test
    fun `given never scanned when allOptimized then returns false`() {
        val analysis = OptimizationAnalysis(
            lastScanTimeMs = null,
            appsNeedingOptimization = 0,
            appsAlreadyOptimized = 5
        )
        assertFalse(analysis.allOptimized)
    }

    @Test
    fun `given scanned with no apps needing optimization and some already optimized when allOptimized then returns true`() {
        val analysis = OptimizationAnalysis(
            lastScanTimeMs = 1_000L,
            appsNeedingOptimization = 0,
            appsAlreadyOptimized = 5
        )
        assertTrue(analysis.allOptimized)
    }

    @Test
    fun `given scanned but apps still need optimization when allOptimized then returns false`() {
        val analysis = OptimizationAnalysis(
            lastScanTimeMs = 1_000L,
            appsNeedingOptimization = 3,
            appsAlreadyOptimized = 2
        )
        assertFalse(analysis.allOptimized)
    }

    @Test
    fun `given scanned with zero already optimized when allOptimized then returns false`() {
        val analysis = OptimizationAnalysis(
            lastScanTimeMs = 1_000L,
            appsNeedingOptimization = 0,
            appsAlreadyOptimized = 0
        )
        assertFalse(analysis.allOptimized)
    }

    // --- progress ---

    @Test
    fun `given totalAppsToScan is zero when progress then returns zero`() {
        val analysis = OptimizationAnalysis(totalAppsToScan = 0, totalAppsScanned = 0)
        assertEquals(0f, analysis.progress, 0.001f)
    }

    @Test
    fun `given half scanned when progress then returns 0 point 5`() {
        val analysis = OptimizationAnalysis(totalAppsToScan = 10, totalAppsScanned = 5)
        assertEquals(0.5f, analysis.progress, 0.001f)
    }

    @Test
    fun `given all scanned when progress then returns 1 point 0`() {
        val analysis = OptimizationAnalysis(totalAppsToScan = 8, totalAppsScanned = 8)
        assertEquals(1.0f, analysis.progress, 0.001f)
    }
}

