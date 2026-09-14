package work.shuyo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat

class EarlyClassAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmIntent = Intent(context, EarlyClassAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("title", intent.getStringExtra("title") ?: "早课提醒")
            putExtra("courseTime", intent.getLongExtra("courseTime", 0L))
        }
        val pending = PendingIntent.getActivity(
            context, intent.getStringExtra("alarmId")?.hashCode() ?: 0, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "早课闹钟", NotificationManager.IMPORTANCE_HIGH
            ).apply { setSound(null, null) })
        }
        manager.notify(
            intent.getStringExtra("alarmId")?.hashCode() ?: 0,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(work.shuyo.app.R.mipmap.ic_launcher)
                .setContentTitle("早课提醒")
                .setContentText(intent.getStringExtra("title") ?: "早课提醒")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setFullScreenIntent(pending, true)
                .build()
        )
    }

    companion object { private const val CHANNEL = "early_class_alarms" }
}
