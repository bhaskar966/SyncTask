package com.bhaskar.synctask.data.platform

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import com.bhaskar.synctask.ReminderReceiver
import com.bhaskar.synctask.data.repository.ReminderRepositoryImpl
import com.bhaskar.synctask.domain.NotificationCalculator
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.platform.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Date

actual class PlatformNotificationScheduler(
    private val context: Context
) : NotificationScheduler, KoinComponent {

    private val repository: ReminderRepository by inject()
    private val notificationCalculator: NotificationCalculator by inject()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    actual override fun scheduleNext() {
        CoroutineScope(Dispatchers.Main).launch {
            println("🔵 Android NotificationScheduler: scheduleNext() called")
            cancelAll()

            val nextNotification = notificationCalculator.getNextNotification() ?: run {
                println("🔴 No upcoming reminders found")
                return@launch
            }

            println("🔵 Next notification: ${nextNotification.title} at ${Date(nextNotification.triggerTime)}")

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("REMINDER_ID", nextNotification.reminderId)
                putExtra("TITLE", nextNotification.title)
                putExtra("BODY", nextNotification.body)
                putExtra("IS_PRE_REMINDER", nextNotification.isPreReminder)
                putExtra("TRIGGER_TIME", nextNotification.triggerTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e("NotificationScheduler", "❌ Cannot schedule exact alarms!")
                    val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(settingsIntent)
                    return@launch
                } else {
                    Log.d("NotificationScheduler", "✅ Exact alarm permission granted")
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextNotification.triggerTime,
                pendingIntent
            )

            Log.d("NotificationScheduler", "✅ Scheduled notification for ${Date(nextNotification.triggerTime)}")
        }
    }

    actual override fun cancelAll() {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    actual override suspend fun handleNotificationDelivered(
        reminderId: String,
        isPreReminder: Boolean
    ) {
        (repository as? ReminderRepositoryImpl)?.handleNotificationDelivered(reminderId, isPreReminder)
        scheduleNext()
    }

    actual override fun cancelNotification(reminderId: String) {
        try {
            println("🚫 Android: Cancelling notification for reminder: $reminderId")

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            // Cancel from notification tray (matches your ReminderReceiver pattern)
            notificationManager.cancel(reminderId.hashCode())
            notificationManager.cancel("pre_$reminderId".hashCode())

            println("✅ Android: Cancelled notification: $reminderId")
        } catch (e: Exception) {
            println("❌ Android: Failed to cancel notification $reminderId: ${e.message}")
            e.printStackTrace()
        }
    }

}