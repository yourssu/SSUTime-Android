package com.yourssu.ssutime.desktop.screen.todo

import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.desktop.ui.util.toTodoDeadlineInstantOrNull
import java.time.Instant

internal fun TodoInfo.isLateSubmissionAvailable(now: Instant): Boolean {
    if (type == TodoType.SUBMITTED || type == TodoType.SUBMITTED_LATE || submittedAt.isNotBlank()) return false
    val due = due_date.toTodoDeadlineInstantOrNull() ?: return false
    val late = lateAt.toTodoDeadlineInstantOrNull() ?: return false
    return late.isAfter(due) && !now.isBefore(due) && now.isBefore(late)
}

internal fun TodoInfo.detailDeadline(now: Instant): String =
    if (isLateSubmissionAvailable(now)) lateAt else due_date
