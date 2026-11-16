package com.yourssu.ssutime.v2.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.todo.TODO_DEADLINE_ZONE_ID
import java.time.LocalDate

private val Context.widgetAnalyticsDataStore by preferencesDataStore(
    name = "widget_analytics",
)

internal enum class WidgetAnalyticsSize(
    val value: String,
) {
    Small("small"),
    Medium("medium"),
    Large("large"),
}

internal suspend fun Context.captureWidgetDisplayOnceDaily(widgetSize: WidgetAnalyticsSize) {
    val key = stringPreferencesKey("widget_display_last_sent_${widgetSize.value}")
    val today = LocalDate.now(TODO_DEADLINE_ZONE_ID).toString()
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
