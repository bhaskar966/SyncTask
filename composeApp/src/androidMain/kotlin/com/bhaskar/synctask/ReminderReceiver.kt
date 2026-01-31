package com.bhaskar.synctask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bhaskar.synctask.platform.NotificationScheduler
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bhaskar.synctask.data.NotificationActionHandler
import com.bhaskar.synctask.notificationDialogsActivities.RescheduleDialogActivity
import com.bhaskar.synctask.notificationDialogsActivities.SnoozeDialogActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val scheduler: NotificationScheduler by inject()

    companion object {
        const val EXTRA_REMINDER_ID = "REMINDER_ID"
        const val EXTRA_TITLE = "TITLE"
        const val EXTRA_BODY = "BODY"
        const val EXTRA_IS_PRE_REMINDER = "IS_PRE_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("REMINDER_ID") ?: return
        val title = intent.getStringExtra("TITLE") ?: "Reminder"
        val body = intent.getStringExtra("BODY") ?: ""
        val isPreReminder = intent.getBooleanExtra("IS_PRE_REMINDER", false)


        showNotification(context, reminderId, title, body, isPreReminder)

        // Handle recurrence if fromCompletion = false
        CoroutineScope(Dispatchers.IO).launch {
            scheduler.handleNotificationDelivered(reminderId, isPreReminder)
        }
    }

    private fun showNotification(
        context: Context,
        reminderId: String,
        title: String,
        body: String,
        isPreReminder: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val channelId = "reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open app
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Add action buttons based on reminder type
        if (isPreReminder) {
            // Pre-reminder: Reschedule, Dismiss
            builder.addAction(
                android.R.drawable.ic_menu_my_calendar,
                "Reschedule",
                createRescheduleActivityPendingIntent(context, reminderId, title)
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                createActionPendingIntent(
                    context,
                    NotificationActionHandler.ACTION_DISMISS,
                    reminderId,
                    title,
                    isPreReminder
                )
            )
        } else {
            // Normal reminder: Complete, Snooze, Dismiss
            builder.addAction(
                android.R.drawable.ic_menu_agenda,
                "Complete",
                createActionPendingIntent(
                    context,
                    NotificationActionHandler.ACTION_COMPLETE,
                    reminderId,
                    title,
                    isPreReminder
                )
            )
            builder.addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze",
                createSnoozeActivityPendingIntent(context, reminderId, title)
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                createActionPendingIntent(
                    context,
                    NotificationActionHandler.ACTION_DISMISS,
                    reminderId,
                    title,
                    isPreReminder
                )
            )
        }

        notificationManager.notify(reminderId.hashCode(), builder.build())
    }


    private fun createSnoozeActivityPendingIntent(
        context: Context,
        reminderId: String,
        title: String
    ): PendingIntent {
        val intent = Intent(context, SnoozeDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
        }

        return PendingIntent.getActivity(
            context,
            "snooze_${reminderId}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ✅ Direct PendingIntent to RescheduleDialogActivity (no trampoline)
    private fun createRescheduleActivityPendingIntent(
        context: Context,
        reminderId: String,
        title: String
    ): PendingIntent {
        val intent = Intent(context, RescheduleDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
        }

        return PendingIntent.getActivity(
            context,
            "reschedule_${reminderId}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // BroadcastReceiver PendingIntent for Complete/Dismiss (no UI needed)
    private fun createActionPendingIntent(
        context: Context,
        action: String,
        reminderId: String,
        title: String,
        isPreReminder: Boolean
    ): PendingIntent {
        val intent = Intent(context, NotificationActionHandler::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder)
        }

        return PendingIntent.getBroadcast(
            context,
            "${action}_${reminderId}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}
