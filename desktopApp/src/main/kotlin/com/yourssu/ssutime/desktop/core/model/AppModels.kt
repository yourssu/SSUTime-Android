package com.yourssu.ssutime.desktop.core.model

import com.yourssu.data.AiSummaryCache
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import kotlinx.serialization.Serializable

typealias AppSubject = SubjectInfo
typealias AppTodo = TodoInfo
typealias AppTodoData = TodoData
typealias AppTodoType = TodoType
typealias AiSummary = AiSummaryCache

@Serializable
data class AppProfile(
    val name: String,
    val department: String,
    val userId: String,
    val email: String,
    val termName: String,
)

val AppTodo.dueDate: String
    get() = due_date

val AppTodoData.aiSummaries: Map<String, AiSummary>
    get() = aiSummaryCache

fun AppTodo.aiSummaryKey(): String =
    "${subject?.id ?: subjectId}:$todoId:${type.name}"

val AppTodo.canRequestAiSummary: Boolean
    get() = type == AppTodoType.ASSIGNMENT &&
        description.isNotBlank() &&
        (subject?.id ?: subjectId) > 0 &&
        todoId > 0
