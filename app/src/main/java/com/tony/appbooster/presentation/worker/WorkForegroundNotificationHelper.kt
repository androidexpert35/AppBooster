package com.tony.appbooster.presentation.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.tony.appbooster.R
import com.tony.appbooster.presentation.activity.MainActivity

/**
 * Builds and manages the foreground notification used by long-running WorkManager jobs.
 *
 * Business purpose:
 * - Provides a single, consistent notification style for "analysis" and "optimization" workers.
 * - Avoids duplicated notification/channel code across workers.
 * - Centralizes Stop-action wiring through [OptimizationWorkerStopReceiver].
 */
object WorkForegroundNotificationHelper {

    /**
     * Ensures the foreground notification channel exists.
     *
     * @param context Context used to register the notification channel.
     */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.optimization_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.optimization_notification_channel_description)
        }

        manager.createNotificationChannel(channel)
    }

    /**
     * Creates a [ForegroundInfo] configured for WorkManager foreground execution.
     *
     * @param context Context used to resolve resources.
     * @param workId WorkManager work id used for the Stop action.
     * @param currentLabel Optional label to show as the notification content text.
     * @return [ForegroundInfo] ready to be passed to `setForeground()`.
     */
    fun createForegroundInfo(
        context: Context,
        workId: String,
        currentLabel: String?
    ): ForegroundInfo {
        val notification = buildNotification(
            context = context,
            workId = workId,
            currentLabel = currentLabel
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Builds the notification used for long running work.
     *
     * @param context Context used to resolve strings and create intents.
     * @param workId WorkManager work id used by the Stop action receiver.
     * @param currentLabel Optional label shown as notification content.
     */
    fun buildNotification(
        context: Context,
        workId: String,
        currentLabel: String?
    ): Notification {
        val title = context.getString(R.string.app_name)
        val contentText = currentLabel?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.optimization_notification_preparing)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, OptimizationWorkerStopReceiver::class.java)
            .putExtra(OptimizationWorkerStopReceiver.EXTRA_WORK_ID, workId)

        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentIntent)
            .addAction(
                NotificationCompat.Action(
                    0,
                    context.getString(R.string.optimization_notification_stop),
                    stopPendingIntent
                )
            )
            .build()
    }

    private const val NOTIFICATION_CHANNEL_ID = "optimization"
    private const val NOTIFICATION_ID = 1001
}
