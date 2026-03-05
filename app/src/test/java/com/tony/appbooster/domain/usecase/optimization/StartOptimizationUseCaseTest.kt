package com.tony.appbooster.domain.usecase.optimization

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.usecase.adb.ConnectAdbUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [StartOptimizationUseCase].
 *
 * Verifies the connection-precondition gate: optimization work is only scheduled
 * when [ConnectAdbUseCase] succeeds, and the connection error propagates otherwise.
 */
class StartOptimizationUseCaseTest {

    private lateinit var connectAdbUseCase: ConnectAdbUseCase
    private lateinit var startOptimizationWorkUseCase: StartOptimizationWorkUseCase
    private lateinit var useCase: StartOptimizationUseCase

    @Before
    fun setUp() {
        connectAdbUseCase = mockk()
        startOptimizationWorkUseCase = mockk()
        useCase = StartOptimizationUseCase(connectAdbUseCase, startOptimizationWorkUseCase)
    }

    @Test
    fun `given connect succeeds and work schedules successfully when invoke then returns success`() = runTest {
        coEvery { connectAdbUseCase() } returns Resource.Success(Unit)
        coEvery { startOptimizationWorkUseCase(AppOptimizationType.SPEED_PROFILE) } returns Resource.Success(Unit)

        val result = useCase(AppOptimizationType.SPEED_PROFILE)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { connectAdbUseCase() }
        coVerify(exactly = 1) { startOptimizationWorkUseCase(AppOptimizationType.SPEED_PROFILE) }
    }

    @Test
    fun `given connect fails when invoke then returns connection error without scheduling work`() = runTest {
        val connectionError = ResourceError.LogicError("Shizuku not running")
        coEvery { connectAdbUseCase() } returns Resource.Error(connectionError)

        val result = useCase(AppOptimizationType.SPEED_PROFILE)

        assertTrue(result is Resource.Error)
        assertEquals(connectionError, (result as Resource.Error).data)
        coVerify(exactly = 0) { startOptimizationWorkUseCase(any()) }
    }

    @Test
    fun `given connect succeeds but work scheduling fails when invoke then returns work error`() = runTest {
        val workError = ResourceError.LogicError("WorkManager unavailable")
        coEvery { connectAdbUseCase() } returns Resource.Success(Unit)
        coEvery { startOptimizationWorkUseCase(any()) } returns Resource.Error(workError)

        val result = useCase(AppOptimizationType.FULL_OPTIMIZATION)

        assertTrue(result is Resource.Error)
        assertEquals(workError, (result as Resource.Error).data)
    }
}

