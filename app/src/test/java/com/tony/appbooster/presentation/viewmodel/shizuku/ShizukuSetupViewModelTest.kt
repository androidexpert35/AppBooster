package com.tony.appbooster.presentation.viewmodel.shizuku

import app.cash.turbine.test
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import com.tony.appbooster.domain.usecase.shizuku.ObserveShizukuStateUseCase
import com.tony.appbooster.domain.usecase.shizuku.OpenShizukuAppUseCase
import com.tony.appbooster.domain.usecase.shizuku.OpenShizukuInstallPageUseCase
import com.tony.appbooster.domain.usecase.shizuku.RefreshShizukuStateUseCase
import com.tony.appbooster.domain.usecase.shizuku.RequestShizukuPermissionUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ShizukuSetupViewModel].
 *
 * Uses a [StandardTestDispatcher] to control coroutine execution and Turbine to
 * assert on StateFlow emissions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShizukuSetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var shizukuStateFlow: MutableStateFlow<ShizukuState>
    private lateinit var observeShizukuState: ObserveShizukuStateUseCase
    private lateinit var refreshShizukuState: RefreshShizukuStateUseCase
    private lateinit var requestPermission: RequestShizukuPermissionUseCase
    private lateinit var openInstallPage: OpenShizukuInstallPageUseCase
    private lateinit var openShizukuApp: OpenShizukuAppUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        shizukuStateFlow = MutableStateFlow(ShizukuState.NotRunning)
        observeShizukuState = mockk()
        refreshShizukuState = mockk()
        requestPermission = mockk()
        openInstallPage = mockk()
        openShizukuApp = mockk()

        every { observeShizukuState() } returns shizukuStateFlow
        coJustRun { refreshShizukuState() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ShizukuSetupViewModel(
        observeShizukuState = observeShizukuState,
        refreshShizukuState = refreshShizukuState,
        requestPermission = requestPermission,
        openInstallPage = openInstallPage,
        openShizukuApp = openShizukuApp
    )

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `given NotRunning initial state when ViewModel created then shizukuState is NotRunning`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotRunning
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuState.NotRunning, vm.uiState.value.shizukuState)
    }

    @Test
    fun `given Ready initial state when ViewModel created then shizukuState is Ready`() = runTest {
        shizukuStateFlow.value = ShizukuState.Ready
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuState.Ready, vm.uiState.value.shizukuState)
    }

    @Test
    fun `given NotInstalled initial state when ViewModel created then shizukuState is NotInstalled`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotInstalled
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuState.NotInstalled, vm.uiState.value.shizukuState)
    }

    @Test
    fun `given PermissionRequired initial state when ViewModel created then shizukuState is PermissionRequired`() = runTest {
        shizukuStateFlow.value = ShizukuState.PermissionRequired
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuState.PermissionRequired, vm.uiState.value.shizukuState)
    }

    @Test
    fun `given Error initial state when ViewModel created then shizukuState is Error`() = runTest {
        val errorState = ShizukuState.Error("something failed")
        shizukuStateFlow.value = errorState
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(errorState, vm.uiState.value.shizukuState)
    }

    // ── State transitions from Flow ───────────────────────────────────────────

    @Test
    fun `given state changes to Ready when observing then uiState reflects latest shizukuState`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotRunning
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val initial = awaitItem()
            assertEquals(ShizukuState.NotRunning, initial.shizukuState)

            shizukuStateFlow.value = ShizukuState.Ready
            val updated = awaitItem()
            assertEquals(ShizukuState.Ready, updated.shizukuState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given state changes to PermissionRequired when observing then uiState reflects latest shizukuState`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotInstalled
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            awaitItem()

            shizukuStateFlow.value = ShizukuState.PermissionRequired
            val updated = awaitItem()
            assertEquals(ShizukuState.PermissionRequired, updated.shizukuState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── refreshState ──────────────────────────────────────────────────────────

    @Test
    fun `given refreshState called when invoked then calls refreshShizukuState use case`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.refreshState()
        advanceUntilIdle()

        coVerify(atLeast = 1) { refreshShizukuState() }
    }

    @Test
    fun `given refreshState called when isCheckingState then briefly becomes true then false`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            awaitItem()

            vm.refreshState()
            val checking = awaitItem()
            assertTrue(checking.isCheckingState)

            val done = awaitItem()
            assertFalse(done.isCheckingState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── UI action delegates ───────────────────────────────────────────────────

    @Test
    fun `given onInstallShizukuClicked when called then invokes openInstallPage use case`() = runTest {
        justRun { openInstallPage() }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onInstallShizukuClicked()

        verify(exactly = 1) { openInstallPage() }
    }

    @Test
    fun `given onOpenShizukuClicked when called then invokes openShizukuApp use case`() = runTest {
        justRun { openShizukuApp() }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onOpenShizukuClicked()

        verify(exactly = 1) { openShizukuApp() }
    }

    @Test
    fun `given onRequestPermissionClicked when called then invokes requestPermission use case`() = runTest {
        coJustRun { requestPermission() }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onRequestPermissionClicked()
        advanceUntilIdle()

        coVerify(exactly = 1) { requestPermission() }
    }

    // ── onResumed ─────────────────────────────────────────────────────────────

    @Test
    fun `given onResumed called when invoked then refreshes state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onResumed()
        advanceUntilIdle()

        coVerify(atLeast = 2) { refreshShizukuState() }
    }
}
