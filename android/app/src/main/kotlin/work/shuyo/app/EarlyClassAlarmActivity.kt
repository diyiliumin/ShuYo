package work.shuyo.app

import android.app.Activity
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class EarlyClassAlarmActivity : Activity() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val title = intent.getStringExtra("title") ?: "早课提醒"
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        layout.addView(TextView(this).apply { text = "早课提醒"; textSize = 28f })
        layout.addView(TextView(this).apply { text = title; textSize = 22f; setPadding(0, 32, 0, 48) })
        layout.addView(Button(this).apply { text = "停止"; setOnClickListener { finish() } })
        setContentView(layout)
        startAlarmSound()
    }

    private fun startAlarmSound() {
        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
        player = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)?.apply {
            isLooping = true
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            start()
        }
    }

    override fun onDestroy() {
        player?.stop(); player?.release(); player = null
        vibrator?.cancel(); vibrator = null
        super.onDestroy()
    }
}
