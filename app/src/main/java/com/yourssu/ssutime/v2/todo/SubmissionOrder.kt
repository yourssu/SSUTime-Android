package com.yourssu.ssutime.v2.todo

import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import java.time.Instant

/** Keep server submission times separate from locally observed completion transitions. */
internal fun TodoData.withSubmissionOrder(
    previous: TodoData,
    now: Instant = Instant.now(),
): TodoData {
    val previousSubmitted = previous.submitted.associateBy { it.submissionIdentity() }
    val previousIncomplete = (previous.todos + previous.hiddenTodos)
        .map { it.submissionIdentity() }.toSet()

    return copy(submitted = submitted.map { todo ->
        val identity = todo.submissionIdentity()
        val previousCompletion = previousSubmitted[identity]
        val observedAt = previousCompletion?.completionObservedAt
            ?.takeIf { it.toTodoDeadlineInstantOrNull() != null }
            ?: todo.completionObservedAt.takeIf { it.toTodoDeadlineInstantOrNull() != null }
            ?: if (previousCompletion == null && identity in previousIncomplete) now.toString() else ""
        todo.copy(completionObservedAt = observedAt).apply { subjectId = todo.subjectId }
    }.sortedBySubmittedAtDescending())
}

// Submission changes the type, so todoUniqueKey cannot identify this transition.
private fun TodoInfo.submissionIdentity(): String {
    val subjectId = subject?.id ?: this.subjectId
    return when {
        todoId > 0 || todoId <= -10000 -> "$subjectId:todo:$todoId"
        moduleItemId > 0 -> "$subjectId:module:$moduleItemId"
        componentId > 0 -> "$subjectId:component:$componentId"
        else -> "$subjectId:title:$title:$due_date"
    }
}
