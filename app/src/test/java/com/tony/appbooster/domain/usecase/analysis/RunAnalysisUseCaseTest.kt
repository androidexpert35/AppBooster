package com.tony.appbooster.domain.usecase.analysis

import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [RunAnalysisUseCase].
 *
 * Verifies that the use case correctly delegates to [AdbRepository.analyzeOptimizationStatus]
 * and propagates both success and error results unchanged.
 */
class RunAnalysisUseCaseTest {

    private lateinit var repository: AdbRepository
    private lateinit var useCase: RunAnalysisUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = RunAnalysisUseCase(repository)
    }

    @Test
    fun `given repository returns analysis data when invoke then returns success with data`() = runTest {
        val analysis = OptimizationAnalysis(
            totalAppsScanned = 50,
            totalAppsToScan = 100,
            appsNeedingOptimization = 30,
            appsAlreadyOptimized = 20,
            lastScanTimeMs = 12345L
        )
        coEvery { repository.analyzeOptimizationStatus(AppOptimizationType.SPEED_PROFILE) } returns
            Resource.Success(analysis)

        val result = useCase(AppOptimizationType.SPEED_PROFILE)

        assertTrue(result is Resource.Success)
        assertEquals(analysis, (result as Resource.Success).data)
        coVerify(exactly = 1) { repository.analyzeOptimizationStatus(AppOptimizationType.SPEED_PROFILE) }
    }

    @Test
    fun `given repository returns error when invoke then propagates error`() = runTest {
        val error = ResourceError.LogicError("ADB not connected")
        coEvery { repository.analyzeOptimizationStatus(any()) } returns Resource.Error(error)

        val result = useCase(AppOptimizationType.FULL_OPTIMIZATION)

        assertTrue(result is Resource.Error)
        assertEquals(error, (result as Resource.Error).data)
    }

    @Test
    fun `given full optimization mode when invoke then passes mode to repository`() = runTest {
        val analysis = OptimizationAnalysis()
        coEvery { repository.analyzeOptimizationStatus(AppOptimizationType.FULL_OPTIMIZATION) } returns
            Resource.Success(analysis)

        useCase(AppOptimizationType.FULL_OPTIMIZATION)

        coVerify(exactly = 1) { repository.analyzeOptimizationStatus(AppOptimizationType.FULL_OPTIMIZATION) }
    }
}

