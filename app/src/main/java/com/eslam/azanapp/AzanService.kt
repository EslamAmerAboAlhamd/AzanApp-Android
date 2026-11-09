package com.eslam.azanapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AzanService : Service() {
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAll()
            return START_NOT_STICKY
        }

        val rawName = intent?.getStringExtra("name") ?: ""
        val lang = Prefs(this).lang
        val name = Strings.prayer(rawName, lang)
        createChannel()

        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, AzanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (lang == "ar") "🕌 حان الآن وقت صلاة $name" else "🕌 It's time for $name prayer")
            .setContentText(if (lang == "ar") "اضغط إيقاف لإنهاء الأذان" else "Tap Stop to end the adhan")
            .setContentIntent(openPi)
            .addAction(0, if (lang == "ar") "إيقاف" else "Stop", stopPi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }

        play()
        return START_NOT_STICKY
    }

    private fun play() {
        stopPlayer()
        val candidates = listOfNotNull(
            Muadhins.resolve(this, Prefs(this)),
            Muadhins.firstBuiltin(this)
        )
        for (uri in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                mp.setDataSource(this, uri)
                mp.prepare()
                mp.setOnCompletionListener { stopAll() }
                mp.start()
                player = mp
                return
            } catch (e: Exception) {
                try { mp.release() } catch (x: Exception) { }
            }
        }
        stopAll()
    }

    private fun stopPlayer() {
        try { player?.stop() } catch (e: Exception) { }
        try { player?.release() } catch (e: Exception) { }
        player = null
    }

    private fun stopAll() {
        stopPlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "الأذان", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    override fun onDestroy() {
        stopPlayer()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "azan_channel"
        const val ACTION_STOP = "com.eslam.azanapp.STOP"
    }
}