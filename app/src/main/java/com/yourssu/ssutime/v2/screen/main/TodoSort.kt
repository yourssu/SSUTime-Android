package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.todo.sortedByDeadlineThenName

internal fun List<TodoInfo>.sortedForMainDisplay(): List<TodoInfo> =
    sortedByDeadlineThenName()
