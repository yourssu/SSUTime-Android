package com.yourssu.ssutime.v2.widget

import android.content.Context
import com.yourssu.data.TodoData
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.screen.main.todoDataStore
import java.time.Instant
import java.util.Locale

private const val WIDGET_REFRESH_STATUS_FAILED = "failed"
private const val WIDGET_REFRESH_STATUS_RUNNING = "running"
private const val WIDGET_REFRESH_REASON_MAX_LENGTH = 42

internal val TodoData.isWidgetRefreshing: Boolean
    get() = lastWidgetRefreshStatus == WIDGET_REFRESH_STATUS_RUNNING

internal val TodoData.widgetRefreshErrorMessage: String?
    get() = lastWidgetRefreshErrorMessage
        .takeIf { lastWidgetRefreshStatus == WIDGET_REFRESH_STATUS_FAILED && it.isNotBlank() }

internal suspend fun Context.markWidgetRefreshRunning() {
    todoDataStore.updateData { currentData ->
        currentData.copy(
            lastWidgetRefreshStatus = WIDGET_REFRESH_STATUS_RUNNING,
            lastWidgetRefreshErrorMessage = "",
            lastWidgetRefreshFinishedAt = "",
        )
    }
}

internal suspend fun Context.markWidgetRefreshFailed(message: String) {
    todoDataStore.updateData { currentData ->
        currentData.copy(
            lastWidgetRefreshStatus = WIDGET_REFRESH_STATUS_FAILED,
            lastWidgetRefreshErrorMessage = message.toWidgetRefreshReason(this),
            lastWidgetRefreshFinishedAt = Instant.now().toString(),
        )
    }
}

private fun String.toWidgetRefreshReason(context: Context): String {
    val reason = trim()
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        .orEmpty()
    val lowerReason = reason.lowercase(Locale.US)

    return when {
        reason.isBlank() -> context.getString(R.string.widget_refresh_error_lms_default)
        "시간이 초과" in reason || "timeout" in lowerReason || "timed out" in lowerReason ->
            context.getString(R.string.widget_refresh_error_timeout)
        "네트워크" in reason ||
            "unable to resolve host" in lowerReason ||
            "failed to connect" in lowerReason ||
            "network" in lowerReason ||
            "socket" in lowerReason ->
            context.getString(R.string.widget_refresh_error_network)
        else -> reason.truncateWidgetReason()
    }
}

private fun String.truncateWidgetReason(): String =
    if (length <= WIDGET_REFRESH_REASON_MAX_LENGTH) {
        this
    } else {
        take(WIDGET_REFRESH_REASON_MAX_LENGTH - 3) + "..."
    }
