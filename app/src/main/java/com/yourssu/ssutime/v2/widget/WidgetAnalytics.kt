package com.yourssu.ssutime.v2.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourssu.ssutime.v2.analytics.Analytics
import java.time.LocalDate
import java.time.ZoneId

private val Context.widgetAnalyticsDataStore by preferencesDataStore(
    name = "widget_analytics",
)

private val WIDGET_ANALYTICS_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

internal enum class WidgetAnalyticsSize(
    val value: String,
) {
    Small("small"),
    Medium("medium"),
    Large("large"),
}

internal suspend fun Context.captureWidgetDisplayOnceDaily(widgetSize: WidgetAnalyticsSize) {
    val key = stringPreferencesKey("widget_display_last_sent_${widgetSize.value}")
    val today = LocalDate.now(WIDGET_ANALYTICS_ZONE_ID).toString()
    var shouldCapture = false

    widgetAnalyticsDataStore.edit { preferences ->
        if (preferences[key] != today) {
            preferences[key] = today
            shouldCapture = true
        }
    }

    if (shouldCapture) {
        Analytics.widgetDisplay(widgetSize.value)
    }
}
