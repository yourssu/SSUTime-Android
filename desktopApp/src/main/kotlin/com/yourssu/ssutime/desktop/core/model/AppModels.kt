package com.yourssu.ssutime.desktop.core.model

import com.yourssu.data.AiSummaryCache
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.isCyber
import kotlinx.serialization.Serializable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

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

fun AppTodo.desktopItemKey(index: Int? = null): String = buildString {
    append(subject?.id ?: subjectId)
    append(':')
    append(todoId)
    append(':')
    append(componentId)
    append(':')
    append(moduleItemId)
    append(':')
    append(type.name)
    append(':')
    append(title)
    append(':')
    append(dueDate)
    append(':')
    append(submittedAt)
    append(':')
    append(url)
    if (index != null) {
        append(':')
        append(index)
    }
}

val AppTodo.canRequestAiSummary: Boolean
    get() = !isCyber() &&
        type == AppTodoType.ASSIGNMENT &&
        description.isNotBlank() &&
        (subject?.id ?: subjectId) > 0 &&
        todoId > 0

fun AppTodo.toLmsUrl(): String {
    if (isCyber()) {
        return url.ifBlank { "https://lms.kcu.ac/atnlcSubj/atnlcApe/list" }
    }
    val currentSubjectId = subject?.id ?: subjectId
    return when (type) {
        AppTodoType.QUIZ -> "https://canvas.ssu.ac.kr/courses/$currentSubjectId/quizzes/$componentId"
        AppTodoType.ASSIGNMENT -> "https://canvas.ssu.ac.kr/courses/$currentSubjectId/assignments/$todoId"
        AppTodoType.COMMONS -> "https://canvas.ssu.ac.kr/courses/$currentSubjectId/modules/items/$moduleItemId"
        else -> url.ifBlank { "https://canvas.ssu.ac.kr/courses/$currentSubjectId" }
    }
}

fun Long.toSimply(): String {
    if (this <= 0L) return "0B"
    if (this < 1024L) return "${this}B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = this.toDouble()
    var unitIndex = 0

    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }

    val decimalFormat = DecimalFormat("0.#", DecimalFormatSymbols(Locale.US))
    var formatted = decimalFormat.format(value)
    if (formatted == "1024" && unitIndex < units.lastIndex) {
        unitIndex++
        formatted = "1"
    }
    return "$formatted${units[unitIndex]}"
}

fun formatVideoDuration(secondsDouble: Double): String {
    val totalSeconds = secondsDouble.toInt()
    if (totalSeconds <= 0) return ""
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return buildString {
        if (hours > 0) {
            append("${hours}시간")
        }
        if (minutes > 0) {
            if (isNotEmpty()) append(" ")
            append("${minutes}분")
        }
        if (seconds > 0 || isEmpty()) {
            if (isNotEmpty()) append(" ")
            append("${seconds}초")
        }
    }
}

