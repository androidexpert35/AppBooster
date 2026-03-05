package com.tony.appbooster.domain.usecase.optimization
import com.tony.appbooster.domain.repository.AdbRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes the raw command output log as a stream.
 *
 * Business purpose:
 * - Allows debug/terminal-style output to be surfaced without directly depending
 *   on repository flows.
 *
 * @property repository Repository providing command output stream.
 */
class ObserveCommandOutputUseCase(
    private val repository: AdbRepository
) {

    /**
     * @return Hot [StateFlow] emitting the latest command output lines.
     */
    operator fun invoke(): StateFlow<List<String>> = repository.commandOutput
}
