package com.yourssu.ssutime.v2.todo

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
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
