package com.tony.appbooster.domain.usecase.optimization

import com.tony.appbooster.domain.model.common.LogEntryType
import com.tony.appbooster.domain.model.common.OptimizationAnalysis
import com.tony.appbooster.domain.model.common.OptimizationLogEntry
import com.tony.appbooster.domain.model.common.OptimizationProgress
import com.tony.appbooster.domain.repository.AdbConnectionState
import com.tony.appbooster.domain.repository.AdbRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the AdbRepository observer use cases:
 * ObserveAdbConnectionStateUseCase, ObserveCommandOutputUseCase,
 * ObserveOptimizationLogEntriesUseCase, ObserveOptimizationProgressUseCase,
 * ObserveOptimizationAnalysisUseCase.
 *
 * Each use case is a thin StateFlow accessor; tests verify that the returned flow
 * is identical to the one exposed by the repository.
 */
class AdbObserverUseCasesTest {

    private lateinit var repository: AdbRepository

    private val connectionStateFlow = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    private val commandOutputFlow = MutableStateFlow<List<String>>(emptyList())
    private val logEntriesFlow = MutableStateFlow<List<OptimizationLogEntry>>(emptyList())
    private val progressFlow = MutableStateFlow(OptimizationProgress())
    private val analysisFlow = MutableStateFlow(OptimizationAnalysis())

    @Before
    fun setUp() {
        repository = mockk()
        every { repository.connectionState } returns connectionStateFlow
        every { repository.commandOutput } returns commandOutputFlow
        every { repository.logEntries } returns logEntriesFlow
        every { repository.optimizationProgress } returns progressFlow
        every { repository.optimizationAnalysis } returns analysisFlow
    }

    // ── ObserveAdbConnectionStateUseCase ──────────────────────────────────────

    @Test
    fun `given ObserveAdbConnectionStateUseCase when invoke then returns repository connectionState`() {
        val useCase = com.tony.appbooster.domain.usecase.adb.ObserveAdbConnectionStateUseCase(repository)
        assertEquals(connectionStateFlow, useCase())
    }

    @Test
    fun `given connection state changes to Connected when observing then emits Connected`() {
        val useCase = com.tony.appbooster.domain.usecase.adb.ObserveAdbConnectionStateUseCase(repository)
        connectionStateFlow.value = AdbConnectionState.Connected

        assertEquals(AdbConnectionState.Connected, useCase().value)
    }

    // ── ObserveCommandOutputUseCase ───────────────────────────────────────────

    @Test
    fun `given ObserveCommandOutputUseCase when invoke then returns repository commandOutput`() {
        val useCase = ObserveCommandOutputUseCase(repository)
        assertEquals(commandOutputFlow, useCase())
    }

    @Test
    fun `given command output has lines when observing then emits those lines`() {
        val lines = listOf("line 1", "line 2")
        commandOutputFlow.value = lines
        val useCase = ObserveCommandOutputUseCase(repository)

        assertEquals(lines, useCase().value)
    }

    // ── ObserveOptimizationLogEntriesUseCase ──────────────────────────────────

    @Test
    fun `given ObserveOptimizationLogEntriesUseCase when invoke then returns repository logEntries`() {
        val useCase = ObserveOptimizationLogEntriesUseCase(repository)
        assertEquals(logEntriesFlow, useCase())
    }

    @Test
    fun `given log entries present when observing then emits those entries`() {
        val entries = listOf(
            OptimizationLogEntry(type = LogEntryType.START, message = "Started")
        )
        logEntriesFlow.value = entries
        val useCase = ObserveOptimizationLogEntriesUseCase(repository)

        assertEquals(entries, useCase().value)
    }

    // ── ObserveOptimizationProgressUseCase ────────────────────────────────────

    @Test
    fun `given ObserveOptimizationProgressUseCase when invoke then returns repository optimizationProgress`() {
        val useCase = ObserveOptimizationProgressUseCase(repository)
        assertEquals(progressFlow, useCase())
    }

    @Test
    fun `given progress is 50 percent when observing then emits that progress`() {
        val progress = OptimizationProgress(progress = 0.5f, isRunning = true)
        progressFlow.value = progress
        val useCase = ObserveOptimizationProgressUseCase(repository)

        assertEquals(0.5f, useCase().value.progress)
    }

    // ── ObserveOptimizationAnalysisUseCase ────────────────────────────────────

    @Test
    fun `given ObserveOptimizationAnalysisUseCase when invoke then returns repository optimizationAnalysis`() {
        val useCase = com.tony.appbooster.domain.usecase.analysis.ObserveOptimizationAnalysisUseCase(repository)
        assertEquals(analysisFlow, useCase())
    }

    @Test
    fun `given analysis has 10 apps needing optimization when observing then emits that count`() {
        val analysis = OptimizationAnalysis(appsNeedingOptimization = 10)
        analysisFlow.value = analysis
        val useCase = com.tony.appbooster.domain.usecase.analysis.ObserveOptimizationAnalysisUseCase(repository)

        assertEquals(10, useCase().value.appsNeedingOptimization)
    }
}

