package com.tony.appbooster.domain.model

import com.tony.appbooster.domain.model.common.AppCompilationInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AppCompilationInfo.Companion.evaluateOptimization] and
 * [AppCompilationInfo.Companion.shouldOptimize].
 *
 * These cover the static business rules that determine whether an app is included in
 * an optimization run, without any Android framework dependencies.
 */
class AppCompilationInfoTest {

    private val now = System.currentTimeMillis()

    // ── evaluateOptimization ─────────────────────────────────────────────────

    @Test
    fun `given no oat file exists when evaluateOptimization then always optimize`() {
        val (needs, reason) = AppCompilationInfo.evaluateOptimization(
            compilerFilter = "speed-profile",
            lastCompilationTimeMs = now - 1_000L,
            lastUpdateTimeMs = null,
            targetFilter = "speed-profile",
            oatFileExists = false
        )
        assertTrue(needs)
        assertNull(reason)
    }

    @Test
    fun `given compiler filter is null when evaluateOptimization then optimize`() {
        val (needs, _) = AppCompilationInfo.evaluateOptimization(
            compilerFilter = null,
            lastCompilationTimeMs = now,
            lastUpdateTimeMs = null,
            targetFilter = "speed",
            oatFileExists = true
        )
        assertTrue(needs)
    }

    @Test
    fun `given app updated after last compilation when evaluateOptimization then optimize`() {
        val compiledAt = now - 10_000L
        val updatedAt = now - 5_000L   // updated after compilation
        val (needs, _) = AppCompilationInfo.evaluateOptimization(
            compilerFilter = "speed",
            lastCompilationTimeMs = compiledAt,
            lastUpdateTimeMs = updatedAt,
            targetFilter = "speed",
            oatFileExists = true
        )
        assertTrue(needs)
    }

    @Test
    fun `given speed filter already applied and recently compiled with speed target when evaluateOptimization then skip`() {
        val recentCompilation = now - (3 * 24 * 60 * 60 * 1000L) // 3 days ago (below threshold)
        val (needs, reason) = AppCompilationInfo.evaluateOptimization(
            compilerFilter = "speed",
            lastCompilationTimeMs = recentCompilation,
            lastUpdateTimeMs = null,
            targetFilter = "speed",
            oatFileExists = true
        )
        assertFalse(needs)
        assertTrue(reason is AppCompilationInfo.SkipReason.RecentlyOptimized)
    }

    @Test
    fun `given speed-profile filter applied and recently compiled with speed-profile target when evaluateOptimization then skip`() {
        val recentCompilation = now - (2 * 24 * 60 * 60 * 1000L) // 2 days ago
        val (needs, reason) = AppCompilationInfo.evaluateOptimization(
            compilerFilter = "speed-profile",
            lastCompilationTimeMs = recentCompilation,
            lastUpdateTimeMs = null,
            targetFilter = "speed-profile",
            oatFileExists = true
        )
        assertFalse(needs)
        assertTrue(reason is AppCompilationInfo.SkipReason.RecentlyOptimized)
    }

    @Test
    fun `given speed filter applied but compiled long ago with speed target when evaluateOptimization then optimize`() {
        val oldCompilation = now - (10 * 24 * 60 * 60 * 1000L) // 10 days ago (above threshold)
        val (needs, reason) = AppCompilationInfo.evaluateOptimization(
            compilerFilter = "speed",
            lastCompilationTimeMs = oldCompilation,
            lastUpdateTimeMs = null,
            targetFilter = "speed",
            oatFileExists = true
        )
        assertTrue(needs)
        assertNull(reason)
    }

    @Test
    fun `given lower quality filter applied when evaluateOptimization with speed target then optimize`() {
        val (needs, _) = AppCompilationInfo.evaluateOptimization(
            compilerFilter = "verify",
            lastCompilationTimeMs = now - 1_000L,
            lastUpdateTimeMs = null,
            targetFilter = "speed",
            oatFileExists = true
        )
        assertTrue(needs)
    }

    // ── shouldOptimize (legacy wrapper) ─────────────────────────────────────

    @Test
    fun `given null compiler filter when shouldOptimize then returns true`() {
        assertTrue(
            AppCompilationInfo.shouldOptimize(
                compilerFilter = null,
                lastCompilationTimeMs = now,
                lastUpdateTimeMs = null,
                targetFilter = "speed"
            )
        )
    }

    @Test
    fun `given speed filter compiled recently with speed target when shouldOptimize then returns false`() {
        val recentCompilation = now - (3 * 24 * 60 * 60 * 1000L)
        assertFalse(
            AppCompilationInfo.shouldOptimize(
                compilerFilter = "speed",
                lastCompilationTimeMs = recentCompilation,
                lastUpdateTimeMs = null,
                targetFilter = "speed"
            )
        )
    }

    @Test
    fun `given verify filter when shouldOptimize then returns true regardless of age`() {
        assertTrue(
            AppCompilationInfo.shouldOptimize(
                compilerFilter = "verify",
                lastCompilationTimeMs = now - 1_000L,
                lastUpdateTimeMs = null,
                targetFilter = "speed-profile"
            )
        )
    }
}

