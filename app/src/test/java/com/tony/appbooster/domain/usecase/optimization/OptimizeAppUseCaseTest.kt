package com.tony.appbooster.domain.usecase.optimization

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
 * Unit tests for [OptimizeAppUseCase].
 *
 * Verifies delegation to [AdbRepository.executeOptimizationCommand] and correct
 * propagation of success/error outcomes for each [AppOptimizationType].
 */
class OptimizeAppUseCaseTest {

    private lateinit var repository: AdbRepository
    private lateinit var useCase: OptimizeAppUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = OptimizeAppUseCase(repository)
    }

    @Test
    fun `given speed profile mode and repository succeeds when invoke then returns success`() = runTest {
        coEvery { repository.executeOptimizationCommand(AppOptimizationType.SPEED_PROFILE) } returns Resource.Success(Unit)

        val result = useCase(AppOptimizationType.SPEED_PROFILE)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { repository.executeOptimizationCommand(AppOptimizationType.SPEED_PROFILE) }
    }

    @Test
    fun `given full optimization mode and repository succeeds when invoke then returns success`() = runTest {
        coEvery { repository.executeOptimizationCommand(AppOptimizationType.FULL_OPTIMIZATION) } returns Resource.Success(Unit)

        val result = useCase(AppOptimizationType.FULL_OPTIMIZATION)

        assertTrue(result is Resource.Success)
    }

    @Test
    fun `given repository returns error when invoke then propagates error`() = runTest {
        val error = ResourceError.LogicError("ADB disconnected")
        coEvery { repository.executeOptimizationCommand(any()) } returns Resource.Error(error)

        val result = useCase(AppOptimizationType.SPEED_PROFILE)

        assertTrue(result is Resource.Error)
        assertEquals(error, (result as Resource.Error).data)
    }
}

