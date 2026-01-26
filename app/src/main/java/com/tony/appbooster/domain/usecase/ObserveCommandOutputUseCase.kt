package com.tony.appbooster.domain.usecase

import com.tony.appbooster.domain.repository.AdbRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the raw command output log as a stream.
 *
 * Business purpose:
 * - Allows debug/terminal-style output to be surfaced without directly depending
 *   on repository flows.
 *
 * @property repository Repository providing command output stream.
 */
class ObserveCommandOutputUseCase @Inject constructor(
    private val repository: AdbRepository
) {

    /**
     * @return Hot [StateFlow] emitting the latest command output lines.
     */
    operator fun invoke(): StateFlow<List<String>> = repository.commandOutput
}
