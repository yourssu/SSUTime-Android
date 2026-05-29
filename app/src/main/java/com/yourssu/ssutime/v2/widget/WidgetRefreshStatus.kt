package com.yourssu.ssutime.v2.widget

import android.content.Context
import com.yourssu.data.TodoData
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
            lastWidgetRefreshErrorMessage = message.toWidgetRefreshReason(),
            lastWidgetRefreshFinishedAt = Instant.now().toString(),
        )
    }
}

private fun String.toWidgetRefreshReason(): String {
    val reason = trim()
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        .orEmpty()
    val lowerReason = reason.lowercase(Locale.US)

    return when {
        reason.isBlank() -> "LMS 정보를 불러오지 못했어요."
        "시간이 초과" in reason || "timeout" in lowerReason || "timed out" in lowerReason ->
            "새로고침 시간이 초과됐어요."
        "네트워크" in reason ||
            "unable to resolve host" in lowerReason ||
            "failed to connect" in lowerReason ||
            "network" in lowerReason ||
            "socket" in lowerReason ->
            "네트워크 연결을 확인해 주세요."
        else -> reason.truncateWidgetReason()
    }
}

private fun String.truncateWidgetReason(): String =
    if (length <= WIDGET_REFRESH_REASON_MAX_LENGTH) {
        this
    } else {
        take(WIDGET_REFRESH_REASON_MAX_LENGTH - 3) + "..."
    }
