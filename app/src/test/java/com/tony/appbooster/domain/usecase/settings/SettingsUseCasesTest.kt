package com.tony.appbooster.domain.usecase.settings

import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.common.ResourceError
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ObserveAppOptimizationTypeUseCase] and [SetAppOptimizationTypeUseCase].
 *
 * Both use cases thin-wrap the repository; tests verify correct delegation and
 * outcome propagation.
 */
class SettingsUseCasesTest {

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        repository = mockk()
    }

    // ── ObserveAppOptimizationTypeUseCase ─────────────────────────────────────

    @Test
    fun `given repository emits speed-profile when ObserveAppOptimizationTypeUseCase invoke then emits same`() = runTest {
        every { repository.observeAppOptimizationType() } returns
            flowOf(Resource.Success(AppOptimizationType.SPEED_PROFILE))

        val useCase = ObserveAppOptimizationTypeUseCase(repository)
        val emissions = useCase().toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is Resource.Success)
        assertEquals(AppOptimizationType.SPEED_PROFILE, (emissions[0] as Resource.Success).data)
        verify(exactly = 1) { repository.observeAppOptimizationType() }
    }

    @Test
    fun `given repository emits error when ObserveAppOptimizationTypeUseCase invoke then emits error`() = runTest {
        val error = ResourceError.DatabaseError("DataStore corrupt")
        every { repository.observeAppOptimizationType() } returns flowOf(Resource.Error(error))

        val useCase = ObserveAppOptimizationTypeUseCase(repository)
        val emissions = useCase().toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is Resource.Error)
        assertEquals(error, (emissions[0] as Resource.Error).data)
    }

    // ── SetAppOptimizationTypeUseCase ─────────────────────────────────────────

    @Test
    fun `given valid type when SetAppOptimizationTypeUseCase invoke then delegates to repository`() = runTest {
        coEvery { repository.setAppOptimizationType(AppOptimizationType.FULL_OPTIMIZATION) } returns
            Resource.Success(Unit)

        val useCase = SetAppOptimizationTypeUseCase(repository)
        val result = useCase(AppOptimizationType.FULL_OPTIMIZATION)

        assertTrue(result is Resource.Success)
        coVerify(exactly = 1) { repository.setAppOptimizationType(AppOptimizationType.FULL_OPTIMIZATION) }
    }

    @Test
    fun `given repository save fails when SetAppOptimizationTypeUseCase invoke then returns error`() = runTest {
        val error = ResourceError.DatabaseError("write failed")
        coEvery { repository.setAppOptimizationType(any()) } returns Resource.Error(error)

        val useCase = SetAppOptimizationTypeUseCase(repository)
        val result = useCase(AppOptimizationType.SPEED_PROFILE)

        assertTrue(result is Resource.Error)
        assertEquals(error, (result as Resource.Error).data)
    }
}

