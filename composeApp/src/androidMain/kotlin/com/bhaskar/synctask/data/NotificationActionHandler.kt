package com.bhaskar.synctask.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.bhaskar.synctask.ReminderReceiver
import com.bhaskar.synctask.domain.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationActionHandler : BroadcastReceiver(), KoinComponent {

    private val repository: ReminderRepository by inject()

    companion object {
        const val ACTION_COMPLETE = "com.bhaskar.synctask.ACTION_COMPLETE"
        const val ACTION_DISMISS = "com.bhaskar.synctask.ACTION_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_ID) ?: return
        val isPreReminder = intent.getBooleanExtra(ReminderReceiver.EXTRA_IS_PRE_REMINDER, false)
        val title = intent.getStringExtra(ReminderReceiver.EXTRA_TITLE) ?: "Reminder"

        when (intent.action) {
            ACTION_COMPLETE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.completeReminder(reminderId)
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "✅ Completed: $title", Toast.LENGTH_SHORT).show()
                    }
                }
                cancelNotification(context, reminderId)
            }

            ACTION_DISMISS -> {
                if (isPreReminder) {
                    // Just cancel notification for pre-reminder
                    cancelNotification(context, reminderId)
                    Toast.makeText(context, "Pre-reminder dismissed", Toast.LENGTH_SHORT).show()
                } else {
                    // Mark as dismissed for normal reminder
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.dismissReminder(reminderId)
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "🚫 Dismissed: $title", Toast.LENGTH_SHORT).show()
                        }
                    }
                    cancelNotification(context, reminderId)
                }
            }
        }
    }

    private fun cancelNotification(context: Context, reminderId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.hashCode())
    }
}