package com.yourssu.ssutime.v2.screen.my

import android.content.Context
import com.yourssu.data.AlertData

object AlertLocalStore {
    private const val PREFS_NAME = "alert_local_store"
    private const val KEY_VALID = "key_valid"
    private const val KEY_ALLOW_SYSTEM_ALERT = "key_allow_system_alert"
    private const val KEY_ALLOW_CALL_ALERT = "key_allow_call_alert"
    private const val KEY_CALLING_ALERT_THRESHOLD = "key_calling_alert_threshold"
    private const val KEY_SHOW_WIDGET_HELPER_BADGE = "key_show_widget_helper_badge"
    private const val KEY_LAST_REMIND_PERIOD = "key_last_remind_period"

    fun getAlertData(context: Context): AlertData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val valid = prefs.getBoolean(KEY_VALID, false)
        return AlertData(
            valid = valid,
            allowSystemAlert = prefs.getBoolean(KEY_ALLOW_SYSTEM_ALERT, false),
            allowCallAlert = prefs.getBoolean(KEY_ALLOW_CALL_ALERT, false),
            callingAlertThresholdMinutes = prefs.getLong(KEY_CALLING_ALERT_THRESHOLD, 60L),
            showWidgetHelperBadge = prefs.getBoolean(KEY_SHOW_WIDGET_HELPER_BADGE, true),
            lastCallAlertRemindPeriod = prefs.getString(KEY_LAST_REMIND_PERIOD, null),
        )
    }

    fun saveAlertData(context: Context, alertData: AlertData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_VALID, alertData.valid)
            .putBoolean(KEY_ALLOW_SYSTEM_ALERT, alertData.allowSystemAlert)
            .putBoolean(KEY_ALLOW_CALL_ALERT, alertData.allowCallAlert)
            .putLong(KEY_CALLING_ALERT_THRESHOLD, alertData.callingAlertThresholdMinutes)
            .putBoolean(KEY_SHOW_WIDGET_HELPER_BADGE, alertData.showWidgetHelperBadge)
            .putString(KEY_LAST_REMIND_PERIOD, alertData.lastCallAlertRemindPeriod)
            .apply()
    }
}
