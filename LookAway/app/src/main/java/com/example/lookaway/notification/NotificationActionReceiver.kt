package com.example.lookaway.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.lookaway.stats.StatsManager

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_LOOKED_AWAY = "com.example.lookaway.ACTION_LOOKED_AWAY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_LOOKED_AWAY) {
            val (currentStreak) = StatsManager.recordLookAwayBreak(context)
            NotificationManagerCompat.from(context).cancel(NotificationHelper.NOTIFICATION_ID)
            Toast.makeText(
                context,
                "Great job looking away! Streak: $currentStreak 🔥",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
