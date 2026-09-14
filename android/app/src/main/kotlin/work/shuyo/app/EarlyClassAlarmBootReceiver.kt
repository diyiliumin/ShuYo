package work.shuyo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EarlyClassAlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Flutter resynchronizes the rolling alarm set when the app next starts.
        // Existing alarms are intentionally left untouched here.
    }
}
