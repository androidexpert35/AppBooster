package com.tony.appbooster.domain.usecase.shizuku

import com.tony.appbooster.domain.client.ShizukuShellClient
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the Shizuku use case family:
 * - [ObserveShizukuStateUseCase]
 * - [RefreshShizukuStateUseCase]
 * - [RequestShizukuPermissionUseCase]
 * - [OpenShizukuInstallPageUseCase]
 * - [OpenShizukuAppUseCase]
 *
 * All tests verify that the use cases delegate correctly to [ShizukuShellClient].
 */
class ShizukuUseCasesTest {

    private val shizukuStateFlow = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    private lateinit var client: ShizukuShellClient

    @Before
    fun setUp() {
        client = mockk()
        every { client.state } returns shizukuStateFlow
    }

    // ── ObserveShizukuStateUseCase ─────────────────────────────────────────────

    @Test
    fun `given client has state flow when ObserveShizukuStateUseCase invoke then returns same flow`() {
        val useCase = ObserveShizukuStateUseCase(client)
        assertEquals(shizukuStateFlow, useCase())
    }

    @Test
    fun `given state is Ready when ObserveShizukuStateUseCase invoke then emits Ready`() {
        shizukuStateFlow.value = ShizukuState.Ready
        val useCase = ObserveShizukuStateUseCase(client)

        assertEquals(ShizukuState.Ready, useCase().value)
    }

    // ── RefreshShizukuStateUseCase ─────────────────────────────────────────────

    @Test
    fun `given RefreshShizukuStateUseCase invoke then calls client refreshState`() = runTest {
        coJustRun { client.refreshState() }
        val useCase = RefreshShizukuStateUseCase(client)

        useCase()

        coVerify(exactly = 1) { client.refreshState() }
    }

    // ── RequestShizukuPermissionUseCase ───────────────────────────────────────

    @Test
    fun `given RequestShizukuPermissionUseCase invoke then calls client requestPermission`() = runTest {
        coJustRun { client.requestPermission() }
        val useCase = RequestShizukuPermissionUseCase(client)

        useCase()

        coVerify(exactly = 1) { client.requestPermission() }
    }

    // ── OpenShizukuInstallPageUseCase ─────────────────────────────────────────

    @Test
    fun `given OpenShizukuInstallPageUseCase invoke then calls client openShizukuInstallPage`() {
        justRun { client.openShizukuInstallPage() }
        val useCase = OpenShizukuInstallPageUseCase(client)

        useCase()

        verify(exactly = 1) { client.openShizukuInstallPage() }
    }

    // ── OpenShizukuAppUseCase ─────────────────────────────────────────────────

    @Test
    fun `given OpenShizukuAppUseCase invoke then calls client openShizukuApp`() {
        justRun { client.openShizukuApp() }
        val useCase = OpenShizukuAppUseCase(client)

        useCase()

        verify(exactly = 1) { client.openShizukuApp() }
    }
}

