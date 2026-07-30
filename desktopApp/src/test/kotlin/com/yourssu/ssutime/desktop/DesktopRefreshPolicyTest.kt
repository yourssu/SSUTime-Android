package com.yourssu.ssutime.desktop

import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopRefreshPolicyTest {
    private val today = LocalDate.of(2026, 7, 30)

    @Test
    fun `a cache loaded today in Korea is reused`() {
        val data = TodoData(loadedAt = "2026-07-29T15:00:00Z")

        assertFalse(shouldRefreshOnOpen(data, today))
    }

    @Test
    fun `an older or malformed cache is refreshed`() {
        assertTrue(
            shouldRefreshOnOpen(
                TodoData(loadedAt = "2026-07-29T14:59:59Z"),
                today,
            ),
        )
        assertTrue(shouldRefreshOnOpen(TodoData(loadedAt = "invalid"), today))
    }

    @Test
    fun `a legacy submitted item without a submission time forces refresh`() {
        val data = TodoData(
            submitted = listOf(
                TodoInfo(
                    todoId = 1,
                    title = "제출 과제",
                    due_date = "2026-07-30T09:00:00Z",
                    type = TodoType.SUBMITTED,
                    subject = null,
                    submittedAt = "",
                ),
            ),
            loadedAt = "2026-07-29T15:00:00Z",
        )

        assertTrue(shouldRefreshOnOpen(data, today))
    }
}
