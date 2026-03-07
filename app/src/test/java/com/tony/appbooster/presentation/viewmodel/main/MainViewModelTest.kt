package com.tony.appbooster.presentation.viewmodel.main

import android.content.Context
import app.cash.turbine.test
import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.model.common.OptimizationResult
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbConnectionState
import com.tony.appbooster.domain.usecase.adb.ConnectAdbUseCase
import com.tony.appbooster.domain.usecase.adb.ObserveAdbConnectionStateUseCase
import com.tony.appbooster.domain.usecase.analysis.ObserveOptimizationAnalysisUseCase
import com.tony.appbooster.domain.usecase.analysis.StartAnalysisUseCase
import com.tony.appbooster.domain.usecase.analysis.StopAnalysisUseCase
import com.tony.appbooster.domain.usecase.optimization.DismissOptimizationResultUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveOptimizationLogEntriesUseCase
import com.tony.appbooster.domain.usecase.optimization.ObserveOptimizationProgressUseCase
import com.tony.appbooster.domain.usecase.optimization.StartOptimizationUseCase
import com.tony.appbooster.domain.usecase.optimization.StopOptimizationUseCase
import com.tony.appbooster.domain.usecase.settings.ObserveAppOptimizationTypeUseCase
import com.tony.appbooster.presentation.navigation.interfaces.NavigationManager
import com.tony.appbooster.presentation.viewmodel.base.UIStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
 * Unit tests for [MainViewModel].
 *
 * Tests verify UDF contract: events → state/effects, correct use-case delegation,
 * and that the current result dismissal flag stays in sync with the active run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val connectionStateFlow = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    private val logEntriesFlow = MutableStateFlow<List<com.tony.appbooster.domain.model.common.OptimizationLogEntry>>(emptyList())
    private val progressFlow = MutableStateFlow(OptimizationProgress())
    private val analysisFlow = MutableStateFlow(OptimizationAnalysis())

    private lateinit var connectAdbUseCase: ConnectAdbUseCase
    private lateinit var stopOptimizationUseCase: StopOptimizationUseCase
    private lateinit var stopAnalysisUseCase: StopAnalysisUseCase
    private lateinit var observeAppOptimizationTypeUseCase: ObserveAppOptimizationTypeUseCase
    private lateinit var observeAdbConnectionStateUseCase: ObserveAdbConnectionStateUseCase
    private lateinit var observeOptimizationLogEntriesUseCase: ObserveOptimizationLogEntriesUseCase
    private lateinit var observeOptimizationProgressUseCase: ObserveOptimizationProgressUseCase
    private lateinit var observeOptimizationAnalysisUseCase: ObserveOptimizationAnalysisUseCase
    private lateinit var startAnalysisUseCase: StartAnalysisUseCase
    private lateinit var startOptimizationUseCase: StartOptimizationUseCase
    private lateinit var dismissOptimizationResultUseCase: DismissOptimizationResultUseCase
    private lateinit var navigationManager: NavigationManager
    private lateinit var appContext: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        connectAdbUseCase = mockk()
        stopOptimizationUseCase = mockk()
        stopAnalysisUseCase = mockk()
        observeAppOptimizationTypeUseCase = mockk()
        observeAdbConnectionStateUseCase = mockk()
        observeOptimizationLogEntriesUseCase = mockk()
        observeOptimizationProgressUseCase = mockk()
        observeOptimizationAnalysisUseCase = mockk()
        startAnalysisUseCase = mockk()
        startOptimizationUseCase = mockk()
        dismissOptimizationResultUseCase = mockk()
        navigationManager = mockk(relaxed = true)
        appContext = mockk()

        every { appContext.getString(any()) } returns "error"
        every { appContext.getString(any(), *anyVararg()) } returns "error"

        every { observeAdbConnectionStateUseCase() } returns connectionStateFlow
        every { observeOptimizationLogEntriesUseCase() } returns logEntriesFlow
        every { observeOptimizationProgressUseCase() } returns progressFlow
        every { observeOptimizationAnalysisUseCase() } returns analysisFlow
        every { observeAppOptimizationTypeUseCase() } returns flowOf(Resource.Success(AppOptimizationType.SPEED_PROFILE))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MainViewModel(
        connectAdbUseCase = connectAdbUseCase,
        stopOptimizationUseCase = stopOptimizationUseCase,
        stopAnalysisUseCase = stopAnalysisUseCase,
        observeAppOptimizationTypeUseCase = observeAppOptimizationTypeUseCase,
        observeAdbConnectionStateUseCase = observeAdbConnectionStateUseCase,
        observeOptimizationLogEntriesUseCase = observeOptimizationLogEntriesUseCase,
        observeOptimizationProgressUseCase = observeOptimizationProgressUseCase,
        observeOptimizationAnalysisUseCase = observeOptimizationAnalysisUseCase,
        startAnalysisUseCase = startAnalysisUseCase,
        startOptimizationUseCase = startOptimizationUseCase,
        dismissOptimizationResultUseCase = dismissOptimizationResultUseCase,
        appContext = appContext,
        navigationManager = navigationManager
    )

    @Test
    fun `given default flows when ViewModel created then uiState data contains disconnected state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(AdbConnectionState.Disconnected, vm.uiState.value.data?.connectionState)
    }

    @Test
    fun `given speed-profile mode emitted when ViewModel created then optimizationMode is SPEED_PROFILE`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(AppOptimizationType.SPEED_PROFILE, vm.uiState.value.data?.optimizationMode)
    }

    @Test
    fun `given connection transitions to Connected when observed then uiState reflects Connected`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        connectionStateFlow.value = AdbConnectionState.Connected
        advanceUntilIdle()

        assertEquals(AdbConnectionState.Connected, vm.uiState.value.data?.connectionState)
    }

    @Test
    fun `given connection transitions to Error when observed then uiState reflects Error`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        connectionStateFlow.value = AdbConnectionState.Error("timeout")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.data?.connectionState is AdbConnectionState.Error)
    }

    @Test
    fun `given OnConnectClicked event when dispatched then calls connectAdbUseCase`() = runTest {
        coEvery { connectAdbUseCase() } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnConnectClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { connectAdbUseCase() }
    }

    @Test
    fun `given connectAdbUseCase returns success when OnConnectClicked then uiState is SUCCESS`() = runTest {
        coEvery { connectAdbUseCase() } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnConnectClicked)
        advanceUntilIdle()

        assertEquals(UIStatus.SUCCESS, vm.uiState.value.status)
    }

    @Test
    fun `given optimization mode set when OnStartOptimizationClicked then calls startOptimizationUseCase`() = runTest {
        coEvery { startOptimizationUseCase(AppOptimizationType.SPEED_PROFILE) } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnStartOptimizationClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { startOptimizationUseCase(AppOptimizationType.SPEED_PROFILE) }
    }

    @Test
    fun `given startOptimization fails when OnStartOptimizationClicked then emits ShowSnackbar effect`() = runTest {
        val error = ResourceError.LogicError("failed")
        coEvery { startOptimizationUseCase(any()) } returns Resource.Error(error)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiEffect.test {
            vm.onEvent(MainUiEvent.OnStartOptimizationClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is MainUiEffect.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given OnStopOptimizationClicked event when dispatched then calls stopOptimizationUseCase`() = runTest {
        coEvery { stopOptimizationUseCase() } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnStopOptimizationClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { stopOptimizationUseCase() }
    }

    @Test
    fun `given OnStopAnalysisClicked event when dispatched then calls stopAnalysisUseCase`() = runTest {
        coEvery { stopAnalysisUseCase() } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnStopAnalysisClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { stopAnalysisUseCase() }
    }

    @Test
    fun `given OnAnalyzeAppsClicked when dispatched then calls startAnalysisUseCase with current mode`() = runTest {
        coEvery { startAnalysisUseCase(AppOptimizationType.SPEED_PROFILE) } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnAnalyzeAppsClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { startAnalysisUseCase(AppOptimizationType.SPEED_PROFILE) }
    }

    @Test
    fun `given startAnalysis fails when OnAnalyzeAppsClicked then emits ShowSnackbar effect`() = runTest {
        val error = ResourceError.LogicError("no connection")
        coEvery { startAnalysisUseCase(any()) } returns Resource.Error(error)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiEffect.test {
            vm.onEvent(MainUiEvent.OnAnalyzeAppsClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is MainUiEffect.ShowSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given completed run when OnDismissOptimizationResultClicked then current result is marked dismissed`() = runTest {
        val runId = 42L
        progressFlow.value = OptimizationProgress(
            runId = runId,
            isRunning = false,
            result = OptimizationResult.Completed
        )
        coEvery { dismissOptimizationResultUseCase() } returns Resource.Success(Unit)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnDismissOptimizationResultClicked)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.data?.isCurrentResultDismissed == true)
        coVerify(exactly = 1) { dismissOptimizationResultUseCase() }
    }

    @Test
    fun `given runId is zero when OnDismissOptimizationResultClicked then does not call dismissUseCase`() = runTest {
        progressFlow.value = OptimizationProgress(runId = 0L)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnDismissOptimizationResultClicked)
        advanceUntilIdle()

        coVerify(exactly = 0) { dismissOptimizationResultUseCase() }
    }

    @Test
    fun `given settings emits FULL_OPTIMIZATION when observed then model reflects that mode`() = runTest {
        every { observeAppOptimizationTypeUseCase() } returns flowOf(Resource.Success(AppOptimizationType.FULL_OPTIMIZATION))
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(AppOptimizationType.FULL_OPTIMIZATION, vm.uiState.value.data?.optimizationMode)
    }

    @Test
    fun `given settings emits error when observed then model keeps previous mode`() = runTest {
        every { observeAppOptimizationTypeUseCase() } returns flowOf(
            Resource.Success(AppOptimizationType.SPEED_PROFILE),
            Resource.Error(ResourceError.DatabaseError("read error"))
        )
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(AppOptimizationType.SPEED_PROFILE, vm.uiState.value.data?.optimizationMode)
    }

    @Test
    fun `given optimization starts and completes when OnStartOptimizationClicked then isStartingOptimization ends as false`() = runTest {
        coEvery { startOptimizationUseCase(any()) } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(MainUiEvent.OnStartOptimizationClicked)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.data?.isStartingOptimization ?: false)
    }
}
