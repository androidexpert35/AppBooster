package com.tony.appbooster.domain.usecase.optimization

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.scheduler.OptimizationWorkScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [StopOptimizationUseCase].
 *
 * Verifies that both WorkManager cancellation and repository-side cancellation
 * are always invoked in the correct order, and that the repository result is
 * propagated to the caller.
 */
class StopOptimizationUseCaseTest {

    private lateinit var scheduler: OptimizationWorkScheduler
    private lateinit var cancelWorkUseCase: CancelOptimizationWorkUseCase
    private lateinit var cancelRepoUseCase: CancelOptimizationUseCase
    private lateinit var useCase: StopOptimizationUseCase

    @Before
    fun setUp() {
        scheduler = mockk(relaxed = true)
        cancelWorkUseCase = CancelOptimizationWorkUseCase(scheduler)
        cancelRepoUseCase = mockk()
        useCase = StopOptimizationUseCase(cancelWorkUseCase, cancelRepoUseCase)
    }

    @Test
    fun `given both cancellations succeed when invoke then returns success`() = runTest {
        coEvery { cancelRepoUseCase() } returns Resource.Success(Unit)

        val result = useCase()

        assertTrue(result is Resource.Success)
        verify(exactly = 1) { scheduler.cancel() }
        coVerify(exactly = 1) { cancelRepoUseCase() }
    }

    @Test
    fun `given repository cancellation fails when invoke then propagates error`() = runTest {
        val error = ResourceError.LogicError("nothing running")
        coEvery { cancelRepoUseCase() } returns Resource.Error(error)

        val result = useCase()

        assertTrue(result is Resource.Error)
        // WorkManager cancel is still invoked regardless
        verify(exactly = 1) { scheduler.cancel() }
    }
}

