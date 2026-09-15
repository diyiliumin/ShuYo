package work.shuyo.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.core.app.NotificationCompat

class EarlyClassAlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra("title") ?: "早课提醒"
        val courseTime = intent?.getLongExtra("courseTime", 0L) ?: 0L
        val alarmId = intent?.getStringExtra("alarmId") ?: "early-class"
        startForeground(NOTIFICATION_ID, buildNotification(title, courseTime, alarmId))
        startAlarmSound()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        stopAlarmPlayback()
        super.onDestroy()
    }

    private fun startAlarmSound() {
        stopAlarmPlayback()

        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.let { deviceVibrator ->
            if (!deviceVibrator.hasVibrator()) return@let
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0L, 700L, 500L),
                    0,
                )
                deviceVibrator.vibrate(
                    effect,
                    android.os.VibrationAttributes.Builder()
                        .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                        .build(),
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                deviceVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0L, 700L, 500L), 0),
                )
            } else {
                @Suppress("DEPRECATION")
                deviceVibrator.vibrate(longArrayOf(0L, 700L, 500L), 0)
            }
        }

        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setWakeMode(this@EarlyClassAlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(this@EarlyClassAlarmService, Settings.System.DEFAULT_ALARM_ALERT_URI)
                isLooping = true
                prepare()
                start()
            }
        }.getOrElse {
            null
        }
    }

    private fun stopAlarm() {
        stopAlarmPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopAlarmPlayback() {
        player?.let { mediaPlayer ->
            runCatching { mediaPlayer.stop() }
            mediaPlayer.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun buildNotification(title: String, courseTime: Long, alarmId: String) =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("早课提醒")
            .setContentText(title)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setContentIntent(alarmPendingIntent(title, courseTime, alarmId, false))
            .setFullScreenIntent(alarmPendingIntent(title, courseTime, alarmId, false), true)
            .addAction(
                R.mipmap.ic_launcher,
                "停止",
                alarmPendingIntent(title, courseTime, alarmId, true),
            )
            .build()

    private fun alarmPendingIntent(
        title: String,
        courseTime: Long,
        alarmId: String,
        stop: Boolean,
    ): PendingIntent {
        val intent = Intent(this, EarlyClassAlarmActivity::class.java).apply {
            action = if (stop) ACTION_STOP else ACTION_SHOW
            putExtra("title", title)
            putExtra("courseTime", courseTime)
            putExtra("alarmId", alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            this,
            if (stop) STOP_PENDING_INTENT_REQUEST_CODE else alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "早课闹钟",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                // Playback is handled by this service with USAGE_ALARM. Keeping
                // the channel silent prevents a second, short notification sound.
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    companion object {
        const val ACTION_START = "work.shuyo.app.EarlyClassAlarmService.START"
        const val ACTION_STOP = "work.shuyo.app.EarlyClassAlarmService.STOP"
        const val ACTION_SHOW = "work.shuyo.app.EarlyClassAlarmService.SHOW"

        private const val CHANNEL = "early_class_alarms"
        private const val NOTIFICATION_ID = 730001
        private const val STOP_PENDING_INTENT_REQUEST_CODE = 730002
    }
}
