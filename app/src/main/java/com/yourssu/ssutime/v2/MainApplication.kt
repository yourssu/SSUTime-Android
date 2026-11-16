package com.yourssu.ssutime.v2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.analytics.captureInstallReferrerIfNeeded
import com.yourssu.ssutime.v2.analytics.identifyStoredAnalyticsUserIfNeeded
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        Analytics.setup(this)
        captureInstallReferrerIfNeeded()
        identifyStoredAnalyticsUserIfNeeded()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val assignmentChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.channel_description)
            }
            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                getString(R.string.call_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.call_channel_description)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(assignmentChannel)
            notificationManager.createNotificationChannel(callChannel)
        }

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }
}
