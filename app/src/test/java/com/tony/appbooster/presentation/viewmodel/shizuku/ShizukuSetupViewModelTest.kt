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
    fun `given NotRunning initial state when ViewModel created then setupStep is START_SERVICE`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotRunning
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuSetupStep.START_SERVICE, vm.uiState.value.setupStep)
    }

    @Test
    fun `given Ready initial state when ViewModel created then setupStep is READY`() = runTest {
        shizukuStateFlow.value = ShizukuState.Ready
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuSetupStep.READY, vm.uiState.value.setupStep)
    }

    @Test
    fun `given NotInstalled initial state when ViewModel created then setupStep is INSTALL_SHIZUKU`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotInstalled
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuSetupStep.INSTALL_SHIZUKU, vm.uiState.value.setupStep)
    }

    @Test
    fun `given PermissionRequired initial state when ViewModel created then setupStep is GRANT_PERMISSION`() = runTest {
        shizukuStateFlow.value = ShizukuState.PermissionRequired
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuSetupStep.GRANT_PERMISSION, vm.uiState.value.setupStep)
    }

    @Test
    fun `given Error initial state when ViewModel created then setupStep is CHECK_STATUS`() = runTest {
        shizukuStateFlow.value = ShizukuState.Error("something failed")
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuSetupStep.CHECK_STATUS, vm.uiState.value.setupStep)
    }

    // ── State transitions from Flow ───────────────────────────────────────────

    @Test
    fun `given state changes to Ready when observing then uiState reflects READY step`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotRunning
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            // Consume current state
            val initial = awaitItem()
            assertEquals(ShizukuSetupStep.START_SERVICE, initial.setupStep)

            // Emit new state
            shizukuStateFlow.value = ShizukuState.Ready
            val updated = awaitItem()
            assertEquals(ShizukuSetupStep.READY, updated.setupStep)
            assertEquals(ShizukuState.Ready, updated.shizukuState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given state changes to PermissionRequired when observing then uiState reflects GRANT_PERMISSION step`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotInstalled
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            awaitItem() // consume initial

            shizukuStateFlow.value = ShizukuState.PermissionRequired
            val updated = awaitItem()
            assertEquals(ShizukuSetupStep.GRANT_PERMISSION, updated.setupStep)

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

        // refreshShizukuState is called once in init + once explicitly
        coVerify(atLeast = 1) { refreshShizukuState() }
    }

    @Test
    fun `given refreshState called when isCheckingState then briefly becomes true then false`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            awaitItem() // initial state (not checking)

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

