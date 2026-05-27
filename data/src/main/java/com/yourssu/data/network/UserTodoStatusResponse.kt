package com.yourssu.data.network

import com.yourssu.data.TodoInfo
import kotlinx.serialization.Serializable

@Serializable
data class UserTodoStatusResponse(
    val todo: ReportedTodoResponse = ReportedTodoResponse(),
    val isCompleted: Boolean = false,
)

@Serializable
data class ReportedTodoResponse(
    val subjectId: Long = -1L,
    val materialCode: Long = -1L,
    val type: String = "",
    val dueDate: String = "",
    val title: String = "",
    val aiSummary: String? = null,
    val estimatedDurationMinutes: Int? = -1,
    val status: String = "",
)

fun ReportedTodoResponse.matches(todo: TodoInfo): Boolean =
    subjectId == (todo.subject?.id ?: todo.subjectId).toLong() &&
        materialCode == todo.todoId.toLong() &&
        type == todo.type.name
