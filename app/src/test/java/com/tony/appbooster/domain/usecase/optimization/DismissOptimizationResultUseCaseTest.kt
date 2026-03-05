package com.tony.appbooster.domain.usecase.optimization

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
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
 * Unit tests for [DismissOptimizationResultUseCase].
 *
 * Verifies correct delegation to [AdbRepository.clearOptimizationResult].
 */
class DismissOptimizationResultUseCaseTest {

    private lateinit var repository: AdbRepository
    private lateinit var useCase: DismissOptimizationResultUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = DismissOptimizationResultUseCase(repository)
    }

    @Test
    fun `given repository clears successfully when invoke then returns success`() = runTest {
        coEvery { repository.clearOptimizationResult() } returns Resource.Success(Unit)

        val result = useCase()

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { repository.clearOptimizationResult() }
    }

    @Test
    fun `given repository returns error when invoke then propagates error`() = runTest {
        val error = ResourceError.LogicError("clear failed")
        coEvery { repository.clearOptimizationResult() } returns Resource.Error(error)

        val result = useCase()

        assertTrue(result is Resource.Error)
        assertEquals(error, (result as Resource.Error).data)
    }
}

