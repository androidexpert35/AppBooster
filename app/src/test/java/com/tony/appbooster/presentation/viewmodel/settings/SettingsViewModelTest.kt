package com.tony.appbooster.presentation.viewmodel.settings

import com.alkemy.boxapp.presentation.navigation.interfaces.NavigationManager
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import com.tony.appbooster.domain.usecase.appinfo.GetAppInfoUseCase
import com.tony.appbooster.domain.usecase.settings.ObserveAppOptimizationTypeUseCase
import com.tony.appbooster.domain.usecase.settings.SetAppOptimizationTypeUseCase
import com.tony.appbooster.domain.usecase.shizuku.ObserveShizukuStateUseCase
import com.tony.appbooster.presentation.screen.settings.model.AppInfo
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
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SettingsViewModel].
 *
 * Verifies that optimization type observation, Shizuku state observation,
 * and persisting selections all update uiState correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val shizukuStateFlow = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)

    private lateinit var navigationManager: NavigationManager
    private lateinit var observeAppOptimizationTypeUseCase: ObserveAppOptimizationTypeUseCase
    private lateinit var setAppOptimizationTypeUseCase: SetAppOptimizationTypeUseCase
    private lateinit var getAppInfoUseCase: GetAppInfoUseCase
    private lateinit var observeShizukuStateUseCase: ObserveShizukuStateUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        navigationManager = mockk(relaxed = true)
        observeAppOptimizationTypeUseCase = mockk()
        setAppOptimizationTypeUseCase = mockk()
        getAppInfoUseCase = mockk()
        observeShizukuStateUseCase = mockk()

        every { observeShizukuStateUseCase() } returns shizukuStateFlow
        every { observeAppOptimizationTypeUseCase() } returns flowOf(Resource.Success(AppOptimizationType.SPEED_PROFILE))
        coEvery { getAppInfoUseCase() } returns Resource.Success(AppInfo("1.0.0", "Alpha"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(
        navigationManager = navigationManager,
        observeAppOptimizationTypeUseCase = observeAppOptimizationTypeUseCase,
        setAppOptimizationTypeUseCase = setAppOptimizationTypeUseCase,
        getAppInfoUseCase = getAppInfoUseCase,
        observeShizukuStateUseCase = observeShizukuStateUseCase
    )

    // ── Optimization type observation ─────────────────────────────────────────

    @Test
    fun `given settings emits SPEED_PROFILE when created then uiState reflects SPEED_PROFILE`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(AppOptimizationType.SPEED_PROFILE, vm.uiState.value.data?.appOptimizationType)
    }

    @Test
    fun `given settings emits FULL_OPTIMIZATION when created then uiState reflects FULL_OPTIMIZATION`() = runTest {
        every { observeAppOptimizationTypeUseCase() } returns flowOf(Resource.Success(AppOptimizationType.FULL_OPTIMIZATION))
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(AppOptimizationType.FULL_OPTIMIZATION, vm.uiState.value.data?.appOptimizationType)
    }

    // ── App info loading ──────────────────────────────────────────────────────

    @Test
    fun `given getAppInfo returns success when created then version name is populated`() = runTest {
        coEvery { getAppInfoUseCase() } returns Resource.Success(AppInfo("2.5.1", "Beta"))
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("2.5.1", vm.uiState.value.data?.appVersionName)
    }

    @Test
    fun `given getAppInfo returns success when created then version channel is populated`() = runTest {
        coEvery { getAppInfoUseCase() } returns Resource.Success(AppInfo("1.0.0", "Stable"))
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("Stable", vm.uiState.value.data?.appVersionChannel)
    }

    // ── Shizuku state observation ─────────────────────────────────────────────

    @Test
    fun `given Shizuku state is NotRunning when created then uiState shizukuState is NotRunning`() = runTest {
        shizukuStateFlow.value = ShizukuState.NotRunning
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ShizukuState.NotRunning, vm.uiState.value.data?.shizukuState)
    }

    @Test
    fun `given Shizuku state changes to Ready when observed then uiState reflects Ready`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        shizukuStateFlow.value = ShizukuState.Ready
        advanceUntilIdle()

        assertEquals(ShizukuState.Ready, vm.uiState.value.data?.shizukuState)
    }

    @Test
    fun `given Shizuku state changes to PermissionRequired when observed then uiState reflects PermissionRequired`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        shizukuStateFlow.value = ShizukuState.PermissionRequired
        advanceUntilIdle()

        assertEquals(ShizukuState.PermissionRequired, vm.uiState.value.data?.shizukuState)
    }

    // ── Persisting optimization type ──────────────────────────────────────────

    @Test
    fun `given FULL_OPTIMIZATION selected when onOptimizationTypeSelected then calls setAppOptimizationTypeUseCase`() = runTest {
        coEvery { setAppOptimizationTypeUseCase(AppOptimizationType.FULL_OPTIMIZATION) } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onOptimizationTypeSelected(AppOptimizationType.FULL_OPTIMIZATION)
        advanceUntilIdle()

        coVerify(exactly = 1) { setAppOptimizationTypeUseCase(AppOptimizationType.FULL_OPTIMIZATION) }
    }

    @Test
    fun `given set optimization type succeeds when persisting then uiState appOptimizationType is updated`() = runTest {
        coEvery { setAppOptimizationTypeUseCase(AppOptimizationType.FULL_OPTIMIZATION) } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onOptimizationTypeSelected(AppOptimizationType.FULL_OPTIMIZATION)
        advanceUntilIdle()

        assertEquals(AppOptimizationType.FULL_OPTIMIZATION, vm.uiState.value.data?.appOptimizationType)
    }

    @Test
    fun `given set optimization type fails when persisting then uiState shows error`() = runTest {
        val error = ResourceError.DatabaseError("write failed")
        coEvery { setAppOptimizationTypeUseCase(any()) } returns Resource.Error(error)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onOptimizationTypeSelected(AppOptimizationType.FULL_OPTIMIZATION)
        advanceUntilIdle()

        // BaseViewModel sets error state on failure
        assertEquals(true, vm.uiState.value.showErrorDialog)
    }

    // ── Event dispatch via onEvent ────────────────────────────────────────────

    @Test
    fun `given OnOptimizationTypeSelected event when dispatched then behaves same as onOptimizationTypeSelected`() = runTest {
        coEvery { setAppOptimizationTypeUseCase(AppOptimizationType.SPEED_PROFILE) } returns Resource.Success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onEvent(SettingsUiEvent.OnOptimizationTypeSelected(AppOptimizationType.SPEED_PROFILE))
        advanceUntilIdle()

        coVerify(exactly = 1) { setAppOptimizationTypeUseCase(AppOptimizationType.SPEED_PROFILE) }
    }
}

