package work.shuyo.app

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class EarlyClassAlarmActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.action == EarlyClassAlarmService.ACTION_STOP) {
            stopAlarm()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val title = intent.getStringExtra("title") ?: "早课提醒"
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        layout.addView(TextView(this).apply { text = "早课提醒"; textSize = 28f })
        layout.addView(TextView(this).apply { text = title; textSize = 22f; setPadding(0, 32, 0, 48) })
        layout.addView(Button(this).apply { text = "停止"; setOnClickListener { stopAlarm() } })
        setContentView(layout)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        setIntent(intent)
        if (intent.action == EarlyClassAlarmService.ACTION_STOP) {
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        stopService(Intent(this, EarlyClassAlarmService::class.java))
        finish()
    }
}
