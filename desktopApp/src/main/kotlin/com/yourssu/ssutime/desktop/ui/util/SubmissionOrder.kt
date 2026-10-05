package com.yourssu.ssutime.desktop.ui.util

import com.yourssu.data.TodoData

internal fun TodoData.withSubmissionOrder(): TodoData =
    copy(submitted = submitted.sortedBySubmittedAtDescending())
