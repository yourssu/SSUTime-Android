package com.yourssu.ssutime.v2.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

internal fun Context.hasPostNotificationsPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

internal fun Context.canPostNotifications(): Boolean {
    val notificationManager = getSystemService(NotificationManager::class.java) ?: return false
    return notificationManager.areNotificationsEnabled() && hasPostNotificationsPermission()
}

internal fun Context.canUseFullScreenIntent(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }
    return getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
}

internal fun Context.appDetailsSettingsIntent(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }

internal fun Context.notificationSettingsIntent(): Intent {
    val notificationSettingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
    } else {
        null
    }

    return notificationSettingsIntent?.takeIf { it.resolveActivity(packageManager) != null }
        ?: appDetailsSettingsIntent()
}

internal fun Context.fullScreenIntentSettingsIntentOrNull(): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return null
    }

    val fullScreenIntentSettings = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = Uri.parse("package:$packageName")
    }

    return fullScreenIntentSettings.takeIf { it.resolveActivity(packageManager) != null }
        ?: appDetailsSettingsIntent().takeIf { it.resolveActivity(packageManager) != null }
}

internal fun Context.fullScreenIntentSettingsIntent(): Intent =
    fullScreenIntentSettingsIntentOrNull() ?: appDetailsSettingsIntent()
