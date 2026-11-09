package com.eslam.azanapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object AzanScheduler {
    fun scheduleNext(ctx: Context, offsetMs: Long = 0) {
        try {
            val prefs = Prefs(ctx)
            val st = PrayerEngine.state(prefs, System.currentTimeMillis() + offsetMs)
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(ctx, AzanReceiver::class.java).putExtra("name", st.nextName)
            val pi = PendingIntent.getBroadcast(
                ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, st.nextTime, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, st.nextTime, pi)
            }
        } catch (e: Exception) { }
    }
}

class AzanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: ""
        ContextCompat.startForegroundService(
            context,
            Intent(context, AzanService::class.java).putExtra("name", name)
        )
        AzanScheduler.scheduleNext(context, 5_000)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AzanScheduler.scheduleNext(context)
    }
}