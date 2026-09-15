package work.shuyo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class EarlyClassAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, EarlyClassAlarmService::class.java).apply {
            action = EarlyClassAlarmService.ACTION_START
            putExtra("title", intent.getStringExtra("title") ?: "早课提醒")
            putExtra("courseTime", intent.getLongExtra("courseTime", 0L))
            putExtra("alarmId", intent.getStringExtra("alarmId") ?: "early-class")
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
