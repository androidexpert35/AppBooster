package com.tony.appbooster.domain.model

import com.tony.appbooster.domain.model.shizuku.ShellResult
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ShizukuState] sealed interface variants and
 * [ShellResult] convenience property.
 */
class ShizukuModelTest {

    // ── ShizukuState variants ─────────────────────────────────────────────────

    @Test
    fun `given NotInstalled when put in list and filtered then can be identified`() {
        val states: List<ShizukuState> = listOf(
            ShizukuState.NotInstalled,
            ShizukuState.NotRunning,
            ShizukuState.Ready
        )
        assertEquals(1, states.count { it is ShizukuState.NotInstalled })
    }

    @Test
    fun `given NotRunning when in a when expression then maps to expected label`() {
        val state: ShizukuState = ShizukuState.NotRunning
        val label = when (state) {
            ShizukuState.NotRunning -> "not_running"
            ShizukuState.Ready -> "ready"
            ShizukuState.NotInstalled -> "not_installed"
            ShizukuState.PermissionRequired -> "permission"
            is ShizukuState.Error -> "error"
        }
        assertEquals("not_running", label)
    }

    @Test
    fun `given PermissionRequired when in a when expression then maps to expected label`() {
        val state: ShizukuState = ShizukuState.PermissionRequired
        val label = when (state) {
            ShizukuState.PermissionRequired -> "permission"
            ShizukuState.NotRunning -> "not_running"
            ShizukuState.Ready -> "ready"
            ShizukuState.NotInstalled -> "not_installed"
            is ShizukuState.Error -> "error"
        }
        assertEquals("permission", label)
    }

    @Test
    fun `given Ready when in a when expression then maps to expected label`() {
        val state: ShizukuState = ShizukuState.Ready
        val label = when (state) {
            ShizukuState.Ready -> "ready"
            ShizukuState.NotRunning -> "not_running"
            ShizukuState.NotInstalled -> "not_installed"
            ShizukuState.PermissionRequired -> "permission"
            is ShizukuState.Error -> "error"
        }
        assertEquals("ready", label)
    }

    @Test
    fun `given Error with message when created then message is accessible`() {
        val state = ShizukuState.Error("some error")
        assertEquals("some error", state.message)
    }

    @Test
    fun `given two Error instances with same message when compared then equal`() {
        val a = ShizukuState.Error("msg")
        val b = ShizukuState.Error("msg")
        assertTrue(a == b)
    }

    // ── ShellResult ────────────────────────────────────────────────────────────

    @Test
    fun `given exit code 0 when isSuccess then returns true`() {
        val result = ShellResult(exitCode = 0, output = "ok", error = "")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `given exit code non-zero when isSuccess then returns false`() {
        val result = ShellResult(exitCode = 1, output = "", error = "permission denied")
        assertFalse(result.isSuccess)
    }

    @Test
    fun `given exit code 127 when isSuccess then returns false`() {
        val result = ShellResult(exitCode = 127, output = "", error = "command not found")
        assertFalse(result.isSuccess)
    }
}

