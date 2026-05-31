package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.TodoInfo
import java.text.Collator
import java.util.Locale

internal fun List<TodoInfo>.sortedForMainDisplay(): List<TodoInfo> {
    val koreanCollator = Collator.getInstance(Locale.KOREAN)
    return sortedWith { left, right ->
        compareValuesBy(left, right) { todo: TodoInfo -> todo.due_date }
            .takeIf { it != 0 }
            ?: koreanCollator.compare(left.sortName(), right.sortName())
                .takeIf { it != 0 }
            ?: koreanCollator.compare(left.title, right.title)
                .takeIf { it != 0 }
            ?: left.todoId.compareTo(right.todoId)
    }
}

private fun TodoInfo.sortName(): String =
    subject?.name
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: title.trim()
