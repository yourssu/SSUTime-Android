package com.yourssu.data

import kotlinx.serialization.Serializable

@Serializable
data class TodoData(
    val todos: List<TodoInfo> = emptyList(),
    val submitted: List<TodoInfo> = emptyList(),
    val aiSummaryCache: Map<String, AiSummaryCache> = emptyMap(),
    val loadedAt: String = "",
    val lastBackgroundRefreshStartedAt: String = "",
    val lastBackgroundRefreshFinishedAt: String = "",
    val lastBackgroundRefreshSuccessAt: String = "",
    val lastBackgroundRefreshStatus: String = "",
    val lastBackgroundRefreshErrorMessage: String = "",
    val lastBackgroundRefreshTodoCount: Int = 0,
    val lastBackgroundRefreshSubmittedCount: Int = 0,
    val lastBackgroundRefreshSemesterCount: Int = 0,
    val lastBackgroundRefreshRequestId: String = "",
    val lastWidgetRefreshStatus: String = "",
    val lastWidgetRefreshErrorMessage: String = "",
    val lastWidgetRefreshFinishedAt: String = "",
    val sentDeadlineReminderKeys: List<String> = emptyList(),
)

@Serializable
data class AiSummaryCache(
    val summary: String = "",
    val estimatedDurationMinutes: Int? = null,
)
