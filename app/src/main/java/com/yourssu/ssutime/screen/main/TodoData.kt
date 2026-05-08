package com.yourssu.ssutime.screen.main

import com.yourssu.data.TodoInfo
import kotlinx.serialization.Serializable

@Serializable
data class TodoData(
    val todos: List<TodoInfo> = emptyList(),
    val submitted: List<TodoInfo> = emptyList(),
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
)
