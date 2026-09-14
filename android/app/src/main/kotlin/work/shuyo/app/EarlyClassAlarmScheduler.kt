package work.shuyo.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

object EarlyClassAlarmScheduler {
    private const val PREFS = "early_class_alarms"
    private const val KEY = "alarms"
    private const val ACTION = "work.shuyo.app.EARLY_CLASS_ALARM"

    fun isAvailable(context: Context): Boolean {
        return true
    }

    fun sync(context: Context, raw: List<Map<String, Any?>>): Int {
      try {
        cancelAll(context)
        val manager = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val saved = JSONArray()
        raw.forEachIndexed { index, item ->
            val time = (item["fireTime"] as? Number)?.toLong() ?: return@forEachIndexed
            if (time <= now) return@forEachIndexed
            val id = item["id"]?.toString() ?: "alarm-$index-$time"
            val requestCode = id.hashCode()
            val intent = Intent(context, EarlyClassAlarmReceiver::class.java).apply {
                action = ACTION
                putExtra("title", item["title"]?.toString() ?: "早课提醒")
                putExtra("courseTime", (item["courseTime"] as? Number)?.toLong() ?: time)
                putExtra("alarmId", id)
            }
            val pending = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP, time, pending)
            }
            saved.put(JSONObject().apply { put("requestCode", requestCode); put("id", id) })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, saved.toString()).apply()
        return saved.length()
      } catch (_: SecurityException) {
        return -1
      }
    }

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = runCatching { JSONArray(prefs.getString(KEY, "[]") ?: "[]") }.getOrDefault(JSONArray())
        for (i in 0 until saved.length()) {
            val code = saved.optJSONObject(i)?.optInt("requestCode") ?: continue
            val intent = Intent(context, EarlyClassAlarmReceiver::class.java).apply { action = ACTION }
            val pending = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            manager.cancel(pending)
            pending.cancel()
        }
        prefs.edit().remove(KEY).apply()
    }
}
