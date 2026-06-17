package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoSortTest {
    @Test
    fun sortedForMainDisplay_matchesMainListOrderTieBreakers() {
        val later = todoInfo(
            todoId = 4,
            title = "가장 늦은 과제",
            dueDate = "2026-06-01T10:00:00+09:00",
            subjectName = "가나다",
        )
        val titleFirst = todoInfo(
            todoId = 3,
            title = "다 과제",
            subjectName = "",
        )
        val subjectFirst = todoInfo(
            todoId = 2,
            title = "나 과제",
            subjectName = "가 과목",
        )
        val subjectSecond = todoInfo(
            todoId = 1,
            title = "가 과제",
            subjectName = "나 과목",
        )

        val sorted = listOf(later, subjectSecond, titleFirst, subjectFirst)
            .sortedForMainDisplay()

        assertEquals(
            listOf(subjectFirst, subjectSecond, titleFirst, later),
            sorted,
        )
    }

    @Test
    fun sortedForMainDisplay_sortsByParsedDeadlineBeforeKoreanName() {
        val earlierDeadline = todoInfo(
            todoId = 1,
            title = "늦은 가나다 과목",
            dueDate = "2026-05-31T23:00:00+09:00",
            subjectName = "하 과목",
        )
        val laterDeadline = todoInfo(
            todoId = 2,
            title = "이른 가나다 과목",
            dueDate = "2026-05-31T15:00:00Z",
            subjectName = "가 과목",
        )

        val sorted = listOf(laterDeadline, earlierDeadline)
            .sortedForMainDisplay()

        assertEquals(
            listOf(earlierDeadline, laterDeadline),
            sorted,
        )
    }

    @Test
    fun sortedForMainDisplay_tiesByKoreanNameTitleThenTodoId() {
        val subjectSecond = todoInfo(
            todoId = 4,
            title = "가 과제",
            subjectName = "나 과목",
        )
        val subjectFirstTitleSecond = todoInfo(
            todoId = 3,
            title = "나 과제",
            subjectName = "가 과목",
        )
        val subjectFirstTitleFirstHigherId = todoInfo(
            todoId = 2,
            title = "가 과제",
            subjectName = "가 과목",
        )
        val subjectFirstTitleFirstLowerId = todoInfo(
            todoId = 1,
            title = "가 과제",
            subjectName = "가 과목",
        )

        val sorted = listOf(
            subjectSecond,
            subjectFirstTitleSecond,
            subjectFirstTitleFirstHigherId,
            subjectFirstTitleFirstLowerId,
        ).sortedForMainDisplay()

        assertEquals(
            listOf(
                subjectFirstTitleFirstLowerId,
                subjectFirstTitleFirstHigherId,
                subjectFirstTitleSecond,
                subjectSecond,
            ),
            sorted,
        )
    }

    @Test
    fun sortedForMainDisplay_treatsDeadlineWithoutOffsetAsSeoulTime() {
        val seoulLocalDeadline = todoInfo(
            todoId = 1,
            title = "서울 시간 과제",
            dueDate = "2026-05-31T09:00:00",
            subjectName = "하 과목",
        )
        val utcDeadline = todoInfo(
            todoId = 2,
            title = "UTC 과제",
            dueDate = "2026-05-31T00:30:00Z",
            subjectName = "가 과목",
        )

        val sorted = listOf(utcDeadline, seoulLocalDeadline)
            .sortedForMainDisplay()

        assertEquals(
            listOf(seoulLocalDeadline, utcDeadline),
            sorted,
        )
    }

    private fun todoInfo(
        todoId: Int,
        title: String,
        dueDate: String = "2026-05-31T09:00:00+09:00",
        subjectName: String,
    ): TodoInfo =
        TodoInfo(
            todoId = todoId,
            title = title,
            due_date = dueDate,
            type = TodoType.ASSIGNMENT,
            subject = SubjectInfo(
                id = todoId,
                name = subjectName,
                professor = "",
            ).takeIf { subjectName.isNotBlank() },
        )
}
