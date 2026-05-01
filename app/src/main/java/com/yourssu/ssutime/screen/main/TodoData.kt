package com.yourssu.ssutime.screen.main

import com.yourssu.data.TodoInfo
import kotlinx.serialization.Serializable

@Serializable
data class TodoData(
    val todos: List<TodoInfo> = emptyList(),
    val submitted: List<TodoInfo> = emptyList(),
    val loadedAt: String = "",
)
