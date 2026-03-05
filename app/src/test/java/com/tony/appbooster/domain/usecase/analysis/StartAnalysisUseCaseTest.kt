package com.tony.appbooster.domain.usecase.analysis

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
 * Unit tests for [StartAnalysisUseCase].
 *
 * Ensures that analysis work is only scheduled when the connection prerequisite passes,
 * and that connection errors are surfaced without scheduling any work.
 */
class StartAnalysisUseCaseTest {

    private lateinit var connectAdbUseCase: ConnectAdbUseCase
    private lateinit var startAnalysisWorkUseCase: StartAnalysisWorkUseCase
    private lateinit var useCase: StartAnalysisUseCase

    @Before
    fun setUp() {
        connectAdbUseCase = mockk()
        startAnalysisWorkUseCase = mockk()
        useCase = StartAnalysisUseCase(connectAdbUseCase, startAnalysisWorkUseCase)
    }

    @Test
    fun `given connect succeeds and work schedules when invoke then returns success`() = runTest {
        coEvery { connectAdbUseCase() } returns Resource.Success(Unit)
        coEvery { startAnalysisWorkUseCase(AppOptimizationType.SPEED_PROFILE) } returns Resource.Success(Unit)

        val result = useCase(AppOptimizationType.SPEED_PROFILE)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { connectAdbUseCase() }
        coVerify(exactly = 1) { startAnalysisWorkUseCase(AppOptimizationType.SPEED_PROFILE) }
    }

    @Test
    fun `given connect fails when invoke then returns error and does not schedule work`() = runTest {
        val error = ResourceError.LogicError("Shizuku not available")
        coEvery { connectAdbUseCase() } returns Resource.Error(error)

        val result = useCase(AppOptimizationType.SPEED_PROFILE)

        assertTrue(result is Resource.Error)
        assertEquals(error, (result as Resource.Error).data)
        coVerify(exactly = 0) { startAnalysisWorkUseCase(any()) }
    }

    @Test
    fun `given connect succeeds but work scheduling fails when invoke then propagates work error`() = runTest {
        val workError = ResourceError.LogicError("WorkManager unavailable")
        coEvery { connectAdbUseCase() } returns Resource.Success(Unit)
        coEvery { startAnalysisWorkUseCase(any()) } returns Resource.Error(workError)

        val result = useCase(AppOptimizationType.FULL_OPTIMIZATION)

        assertTrue(result is Resource.Error)
        assertEquals(workError, (result as Resource.Error).data)
    }
}

