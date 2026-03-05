package com.tony.appbooster.domain.usecase.adb

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
 * Unit tests for [ConnectAdbUseCase].
 *
 * Verifies that the use case correctly delegates to [AdbRepository.ensureConnected]
 * and propagates both success and error outcomes unchanged.
 */
class ConnectAdbUseCaseTest {

    private lateinit var repository: AdbRepository
    private lateinit var useCase: ConnectAdbUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = ConnectAdbUseCase(repository)
    }

    @Test
    fun `given repository returns success when invoke then returns success`() = runTest {
        coEvery { repository.ensureConnected() } returns Resource.Success(Unit)

        val result = useCase()

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { repository.ensureConnected() }
    }

    @Test
    fun `given repository returns error when invoke then returns error`() = runTest {
        val error = ResourceError.LogicError("Shizuku not ready")
        coEvery { repository.ensureConnected() } returns Resource.Error(error)

        val result = useCase()

        assertTrue(result is Resource.Error)
        assertEquals(error, (result as Resource.Error).data)
        coVerify(exactly = 1) { repository.ensureConnected() }
    }
}

