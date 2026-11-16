package com.yourssu.ssutime.v2.todo

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.R

@StringRes
internal fun TodoType.labelResId(): Int = when (this) {
    TodoType.COMMONS -> R.string.todo_type_lecture
    TodoType.ASSIGNMENT -> R.string.todo_type_assignment
    TodoType.QUIZ -> R.string.todo_type_quiz
    TodoType.SUBMITTED -> R.string.todo_type_submitted
    TodoType.SUBMITTED_LATE -> R.string.todo_type_submitted_late
}

@Composable
internal fun TodoType.localizedLabel(): String = stringResource(labelResId())

internal fun TodoType.localizedLabel(context: Context): String = context.getString(labelResId())

internal fun TodoType.badgeBackgroundColor(): Color = when (this) {
    TodoType.COMMONS -> Color(0xFFF7DBF7)
    TodoType.QUIZ -> Color(0xFFFFD7C2)
    TodoType.ASSIGNMENT -> Color(0xFFD8E5F7)
    else -> Color(0xFFF7DBF7)
}

internal fun TodoType.badgeTextColor(): Color = when (this) {
    TodoType.COMMONS -> Color(0xFFFF39D0)
    TodoType.QUIZ -> Color(0xFFFF5F0B)
    TodoType.ASSIGNMENT -> Color(0xFF007BFF)
    else -> Color(0xFFFF39D0)
}

internal fun String?.toTodoTypeOrNull(): TodoType? =
    TodoType.entries.firstOrNull { type ->
        equals(type.name, ignoreCase = true) || this == type.kor
    }
