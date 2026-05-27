package com.yourssu.data.network

import com.yourssu.data.TodoInfo
import kotlinx.serialization.Serializable

@Serializable
data class TodoReportRequest(
    val subjectId: Long,
    val materialCode: Long,
    val type: String,
    val dueDate: String,
    val title: String,
)

fun TodoInfo.toTodoReportRequest(): TodoReportRequest =
    TodoReportRequest(
        subjectId = (subject?.id ?: subjectId).toLong(),
        materialCode = todoId.toLong(),
        type = type.name,
        dueDate = due_date,
        title = title,
    )
