package com.yourssu.ssutime.v2.todo

import com.yourssu.data.TodoData

internal fun TodoData.withSubmissionOrder(): TodoData =
    copy(submitted = submitted.sortedBySubmittedAtDescending())
