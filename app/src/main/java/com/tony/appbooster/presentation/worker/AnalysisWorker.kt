package com.tony.appbooster.presentation.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tony.appbooster.domain.model.common.Resource
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.repository.AdbRepository
import com.tony.appbooster.domain.usecase.EnsureAdbConnectedUseCase
import com.tony.appbooster.domain.usecase.RunAnalysisUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/**
 * Foreground [CoroutineWorker] that runs the pre-optimization analysis.
 *
 * Business purpose:
 * - Keeps analysis running even when the app is backgrounded.
 * - Uses the same foreground notification pattern as [OptimizationWorker] for UI consistency.
 * - Exposes a Stop action that cancels this Worker.
 *
 * @property repository Repository coordinating shell connection and analysis progress.
 * @constructor Creates the worker with injected dependencies.
 */
@HiltWorker
class AnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AdbRepository,
    private val ensureAdbConnectedUseCase: EnsureAdbConnectedUseCase,
    private val runAnalysisUseCase: RunAnalysisUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val optimizationModeRaw = inputData.getString(KEY_OPTIMIZATION_MODE)
            ?: return@coroutineScope Result.failure()

        val mode = parseOptimizationMode(optimizationModeRaw) ?: return@coroutineScope Result.failure()

        WorkForegroundNotificationHelper.ensureChannel(applicationContext)

        // Start foreground immediately.
        setForeground(
            WorkForegroundNotificationHelper.createForegroundInfo(
                context = applicationContext,
                workId = id.toString(),
                currentLabel = null,
                progressCurrent = 0,
                progressTotal = 0
            )
        )

        // Update notification whenever the current package/progress changes.
        val notificationJob: Job = launch {
            repository.optimizationAnalysis
                .distinctUntilChangedBy { analysis ->
                    "${analysis.currentPackage}|${analysis.totalAppsScanned}|${analysis.totalAppsToScan}"
                }
                .collect { analysis ->
                    val percent = (analysis.progress * 100f).toInt().coerceIn(0, 100)
                    setForeground(
                        WorkForegroundNotificationHelper.createForegroundInfo(
                            context = applicationContext,
                            workId = id.toString(),
                            currentLabel = analysis.currentPackage.ifBlank { null },
                            progressPercent = if (analysis.totalAppsToScan > 0) percent else null,
                            progressCurrent = analysis.totalAppsScanned,
                            progressTotal = analysis.totalAppsToScan
                        )
                    )
                }
        }

        try {
            when (ensureAdbConnectedUseCase()) {
                is Resource.Success -> Unit
                is Resource.Error -> return@coroutineScope Result.failure()
            }

            if (isStopped) {
                repository.cancelAnalysis()
                return@coroutineScope Result.success()
            }

            when (runAnalysisUseCase(mode)) {
                is Resource.Success -> Result.success()
                is Resource.Error -> Result.failure()
            }
        } catch (_: CancellationException) {
            // WorkManager cancellation (e.g., notification stop) lands here.
            repository.cancelAnalysis()
            Result.success()
        } finally {
            notificationJob.cancel()
        }
    }

    private fun parseOptimizationMode(value: String): AppOptimizationType? {
        return when (value) {
            AppOptimizationType.SPEED_PROFILE.value -> AppOptimizationType.SPEED_PROFILE
            AppOptimizationType.FULL_OPTIMIZATION.value -> AppOptimizationType.FULL_OPTIMIZATION
            else -> null
        }
    }

    companion object {
        const val KEY_OPTIMIZATION_MODE = "optimization_mode"

        private const val UNIQUE_WORK_NAME = "analysis_work"
        const val TAG = "analysis"

        /**
         * Enqueues a unique analysis worker.
         *
         * @param context Context used to enqueue work.
         * @param mode Optimization mode used for analysis criteria.
         */
        fun enqueue(context: Context, mode: AppOptimizationType) {
            val request = androidx.work.OneTimeWorkRequestBuilder<AnalysisWorker>()
                .setInputData(
                    androidx.work.workDataOf(KEY_OPTIMIZATION_MODE to mode.value)
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request)
        }

        /**
         * Cancels the currently running analysis worker, if any.
         *
         * @param context Context used to cancel work.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
