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
import com.tony.appbooster.domain.usecase.OptimizeAppUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/**
 * Foreground [CoroutineWorker] that runs the app optimization workflow.
 *
 * Business purpose:
 * - Keeps optimization running even when the app is backgrounded.
 * - Shows a persistent notification with the currently optimized package.
 * - Exposes a Stop action that cancels this Worker. Cancellation also requests repository-side
 *   cancellation so the UI reflects the stop immediately when opened.
 *
 * The Worker reuses the singleton [AdbRepository] instance; since the UI subscribes to its
 * progress/log flows, state stays in sync across app/worker.
 *
 * @property repository Repository coordinating shell connection and optimization progress.
 * @constructor Creates the worker with injected dependencies.
 */
@HiltWorker
class OptimizationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AdbRepository,
    private val ensureAdbConnectedUseCase: EnsureAdbConnectedUseCase,
    private val optimizeAppUseCase: OptimizeAppUseCase
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
                progressPercent = null,
                progressCurrent = null,
                progressTotal = null
            )
        )

        // Update notification whenever the current package/progress changes.
        val notificationJob: Job = launch {
            repository.optimizationProgress
                .distinctUntilChangedBy { progress ->
                    // Avoid spam updates; update when either current app or computed percent changes.
                    "${progress.currentAppPackage}|${(progress.progress * 100f).toInt()}|${progress.processedCount}|${progress.totalCount}"
                }
                .collect { progress ->
                    val percent = (progress.progress * 100f).toInt().coerceIn(0, 100)
                    setForeground(
                        WorkForegroundNotificationHelper.createForegroundInfo(
                            context = applicationContext,
                            workId = id.toString(),
                            currentLabel = progress.currentAppPackage.ifBlank { null },
                            progressPercent = if (progress.totalCount > 0) percent else null,
                            progressCurrent = progress.processedCount,
                            progressTotal = progress.totalCount
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
                repository.cancelOptimization()
                return@coroutineScope Result.success()
            }

            when (optimizeAppUseCase(mode)) {
                is Resource.Success -> Result.success()
                is Resource.Error -> Result.failure()
            }
        } catch (_: CancellationException) {
            // WorkManager cancellation (e.g., notification stop) lands here.
            repository.cancelOptimization()
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

        /**
         * Enqueues a unique optimization worker.
         *
         * @param context Context used to enqueue work.
         * @param mode Optimization mode to run.
         */
        fun enqueue(context: Context, mode: AppOptimizationType) {
            val request = androidx.work.OneTimeWorkRequestBuilder<OptimizationWorker>()
                .setInputData(
                    androidx.work.workDataOf(KEY_OPTIMIZATION_MODE to mode.value)
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request)
        }

        /**
         * Cancels the currently running optimization worker, if any.
         *
         * @param context Context used to cancel work.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        private const val UNIQUE_WORK_NAME = "optimization_work"
        const val TAG = "optimization"
    }
}
