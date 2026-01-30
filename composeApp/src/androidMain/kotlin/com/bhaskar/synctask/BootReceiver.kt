package com.bhaskar.synctask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bhaskar.synctask.platform.NotificationScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val scheduler: NotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduler.scheduleNext()
            Log.d("BootReceiver", "Rescheduled notifications after device reboot")
        }
    }
}