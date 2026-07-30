package com.yourssu.ssutime.v2.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter

private const val TAG = "CallAlertRinger"
private const val CALL_RING_TIMEOUT_MILLIS = 60_000L

internal object CallAlertRinger {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrationToken = 0

    @Synchronized
    fun start(context: Context) {
        val appContext = context.applicationContext
        vibrationToken += 1
        val currentToken = vibrationToken

        startRingtone(appContext)
        startVibration(appContext)

        handler.postDelayed({
            synchronized(this) {
                if (vibrationToken == currentToken) {
                    stop(appContext)
                }
            }
        }, CALL_RING_TIMEOUT_MILLIS)
    }

    @Synchronized
    fun stop(context: Context) {
        vibrationToken += 1
        mediaPlayer?.runCatching {
            stop()
            release()
        }?.onFailure { exception ->
            SentryExceptionReporter.capture(exception)
        }
        mediaPlayer = null
        context.applicationContext.getCallAlertVibrator()?.cancel()
    }

    private fun startRingtone(context: Context) {
        if (mediaPlayer != null) {
            return
        }

        runCatching {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { exception ->
            SentryExceptionReporter.capture(exception)
            Log.w(TAG, "통화 알림 벨소리를 재생하지 못했습니다.", exception)
            mediaPlayer = null
        }
    }

    private fun startVibration(context: Context) {
        val vibrator = context.getCallAlertVibrator() ?: return
        val pattern = longArrayOf(0, 1_000, 700, 1_000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun Context.getCallAlertVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
}
