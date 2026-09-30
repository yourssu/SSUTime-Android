package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import io.github.chlwhdtn03.data.Lms.Subject
import io.github.chlwhdtn03.data.Lms.Submission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class LmsSmartCacheMergeTest {

    private fun createTestSubject(
        id: Int,
        name: String,
        todoList: List<io.github.chlwhdtn03.data.Lms.TodoList> = emptyList(),
        submissions: List<Submission> = emptyList(),
    ) = Subject(
        id = id,
        termId = 1,
        termName = "2026-1학기",
        name = name,
        professor = "교수님",
        totalStudents = 30,
        todoList = todoList,
        attendances = emptyList(),
        discussions = emptyList(),
        submissions = submissions,
        scoredAssignments = emptyList(),
    )

    @Test
    fun buildTodoData_preservesUnfinishedTodo_whenNetworkTemporarilyDropsIt() {
        val subjectId = 101
        val subjectInfo = SubjectInfo(subjectId, "운영체제", "교수님")
        val futureDueDate = Instant.now().plus(2, ChronoUnit.DAYS).toString()

        val previousTodo = TodoInfo(
            todoId = 999,
            title = "과제1 (네트워크 누락 시뮬레이션)",
            due_date = futureDueDate,
            type = TodoType.ASSIGNMENT,
            subject = subjectInfo,
        )

        val previousData = TodoData(
            todos = listOf(previousTodo),
            subjects = listOf(subjectInfo),
        )

        // 이번 새로고침: 과목은 존재하지만 todoList와 submissions가 비어있음 (일시적 조회 누락)
        val currentSubjects = listOf(
            createTestSubject(
                id = subjectId,
                name = "운영체제",
                todoList = emptyList(),
                submissions = emptyList(),
            )
        )

        val result = LmsRefreshRepository.buildTodoData(
            subjects = currentSubjects,
            subjectInfos = listOf(subjectInfo),
            previousData = previousData,
            loadedAt = Instant.now().toString(),
        )

        // 누락되었던 과제가 스마트 머지를 통해 보존되어야 함
        assertEquals(1, result.todos.size)
        assertEquals("과제1 (네트워크 누락 시뮬레이션)", result.todos.first().title)
    }

    @Test
    fun buildTodoData_doesNotPreserve_whenTodoIsSubmitted() {
        val subjectId = 101
        val subjectInfo = SubjectInfo(subjectId, "운영체제", "교수님")
        val futureDueDate = Instant.now().plus(2, ChronoUnit.DAYS).toString()

        val previousTodo = TodoInfo(
            todoId = 999,
            title = "과제1",
            due_date = futureDueDate,
            type = TodoType.ASSIGNMENT,
            subject = subjectInfo,
        )

        val previousData = TodoData(
            todos = listOf(previousTodo),
            subjects = listOf(subjectInfo),
        )

        // 이번 새로고침: 학생이 제출하여 submissions에 들어옴
        val currentSubjects = listOf(
            createTestSubject(
                id = subjectId,
                name = "운영체제",
                todoList = emptyList(),
                submissions = listOf(
                    Submission(
                        assignment_id = 999,
                        cached_due_date = futureDueDate,
                        submitted_at = Instant.now().toString(),
                        workflow_state = "submitted",
                    ).apply {
                        name = "과제1"
                    }
                ),
            )
        )

        val result = LmsRefreshRepository.buildTodoData(
            subjects = currentSubjects,
            subjectInfos = listOf(subjectInfo),
            previousData = previousData,
            loadedAt = Instant.now().toString(),
        )

        // 제출 완료되었으므로 미완료 목록(todos)에서는 빠져야 하고 submitted에 들어가야 함
        assertTrue(result.todos.isEmpty())
        assertEquals(1, result.submitted.size)
        assertEquals(999, result.submitted.first().todoId)
    }

    @Test
    fun buildTodoData_doesNotPreserve_whenDeadlinePassed() {
        val subjectId = 101
        val subjectInfo = SubjectInfo(subjectId, "운영체제", "교수님")
        val pastDueDate = Instant.now().minus(2, ChronoUnit.DAYS).toString()

        val previousTodo = TodoInfo(
            todoId = 999,
            title = "기한 지난 과제",
            due_date = pastDueDate,
            type = TodoType.ASSIGNMENT,
            subject = subjectInfo,
        )

        val previousData = TodoData(
            todos = listOf(previousTodo),
            subjects = listOf(subjectInfo),
        )

        val currentSubjects = listOf(
            createTestSubject(
                id = subjectId,
                name = "운영체제",
                todoList = emptyList(),
                submissions = emptyList(),
            )
        )

        val result = LmsRefreshRepository.buildTodoData(
            subjects = currentSubjects,
            subjectInfos = listOf(subjectInfo),
            previousData = previousData,
            loadedAt = Instant.now().toString(),
        )

        // 마감 기한이 지났으므로 미완료 목록에 보존되지 않아야 함
        assertTrue(result.todos.isEmpty())
    }

    @Test
    fun buildTodoData_doesNotPreserve_whenSubjectNotEnrolledInCurrentTerm() {
        val otherSubjectInfo = SubjectInfo(9999, "지난 학기 과목", "교수님")
        val futureDueDate = Instant.now().plus(2, ChronoUnit.DAYS).toString()

        val previousTodo = TodoInfo(
            todoId = 123,
            title = "지난 학기 과제",
            due_date = futureDueDate,
            type = TodoType.ASSIGNMENT,
            subject = otherSubjectInfo,
        )

        val previousData = TodoData(
            todos = listOf(previousTodo),
            subjects = listOf(otherSubjectInfo),
        )

        // 이번 학기 과목 목록에는 과목 101만 존재함
        val currentSubjects = listOf(
            createTestSubject(
                id = 101,
                name = "이번 학기 과목",
                todoList = emptyList(),
                submissions = emptyList(),
            )
        )

        val result = LmsRefreshRepository.buildTodoData(
            subjects = currentSubjects,
            subjectInfos = listOf(SubjectInfo(101, "이번 학기 과목", "교수님")),
            previousData = previousData,
            loadedAt = Instant.now().toString(),
        )

        // 다른 학기 과목의 과제는 보존되지 않아야 함
        assertTrue(result.todos.isEmpty())
    }

    @Test
    fun buildTodoData_updatesCyberTodoDeadlineWithoutDuplication() {
        val cyberSubjectInfo = SubjectInfo(-100001, "인공지능개론", "교수님")
        val cyberTodoId = -15001

        // 이전 캐시: 02:00:00 (새벽 2시) 기준 due_date
        val oldDeadline = "2026-03-15T17:00:00Z"
        val previousTodo = TodoInfo(
            todoId = cyberTodoId,
            title = "1주차 1강",
            due_date = oldDeadline,
            type = TodoType.COMMONS,
            subject = cyberSubjectInfo,
        )

        val previousData = TodoData(
            todos = listOf(previousTodo),
            subjects = listOf(cyberSubjectInfo),
        )

        // 이번 새로고침: 14:00:00 (낮 2시)로 수정된 새 due_date
        val newDeadline = "2026-03-15T05:00:00Z"
        val currentTodo = TodoInfo(
            todoId = cyberTodoId,
            title = "1주차 1강",
            due_date = newDeadline,
            type = TodoType.COMMONS,
            subject = cyberSubjectInfo,
        )

        val result = LmsRefreshRepository.buildTodoData(
            subjects = emptyList(),
            subjectInfos = emptyList(),
            previousData = previousData,
            loadedAt = Instant.now().toString(),
            isCyberConnected = true,
            cyberResult = com.yourssu.ssutime.v2.screen.cyber.CyberTodoResult(
                todos = listOf(currentTodo),
                subjects = listOf(cyberSubjectInfo),
            ),
        )

        // 중복되지 않고 최신 마감기한(newDeadline)의 1개만 남아야 함
        assertEquals(1, result.todos.size)
        assertEquals(newDeadline, result.todos.first().due_date)
    }
}
