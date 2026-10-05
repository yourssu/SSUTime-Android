package com.yourssu.ssutime.desktop

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoData
import com.yourssu.data.TodoType
import com.yourssu.ssutime.desktop.core.lms.toAppTodoData
import com.yourssu.ssutime.desktop.core.cyber.CyberTodoMapper
import com.yourssu.ssutime.desktop.core.cyber.CyberTodoResult
import io.github.chlwhdtn03.data.Cyber.CyberEvaluation
import io.github.chlwhdtn03.data.Cyber.CyberEvaluationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CyberEvaluationStatusTest {
    private val subject = SubjectInfo(-100001, "AI 기술 산업 응용 및 설계", "교수님")

    @Test
    fun takenQuizMovesToSubmitted() {
        for (status in listOf("응시", " 응시 ", "응시완료", "완료")) {
            val (todos, submitted) = CyberTodoMapper.mapEvaluationsToTodos(subject, listOf(evaluation(status)))
            assertTrue(todos.isEmpty())
            assertEquals(TodoType.SUBMITTED, submitted.single().type)
        }
    }

    @Test
    fun untakenQuizRemainsIncomplete() {
        for (status in listOf("미응시", "미 응시", "미완료", "미응시완료", "")) {
            val (todos, submitted) = CyberTodoMapper.mapEvaluationsToTodos(subject, listOf(evaluation(status)))
            assertEquals(TodoType.QUIZ, todos.single().type)
            assertTrue(submitted.isEmpty())
        }
    }

    @Test
    fun submittedAssignmentMovesToSubmittedButUnsubmittedAssignmentRemains() {
        for (status in listOf("제출", " 제출 ", "제출완료")) {
            val assignment = evaluation(status).copy(type = CyberEvaluationType.ASSIGNMENT, typeCode = "03")
            val (todos, submitted) = CyberTodoMapper.mapEvaluationsToTodos(subject, listOf(assignment))
            assertTrue(todos.isEmpty())
            assertEquals(TodoType.SUBMITTED, submitted.single().type)
        }
        for (status in listOf("미제출", "미 제출", "미제출완료", "제출중", "")) {
            val assignment = evaluation(status).copy(type = CyberEvaluationType.ASSIGNMENT, typeCode = "03")
            val (todos, submitted) = CyberTodoMapper.mapEvaluationsToTodos(subject, listOf(assignment))
            assertEquals(TodoType.ASSIGNMENT, todos.single().type)
            assertTrue(submitted.isEmpty())
        }
    }

    @Test
    fun lateCompletionStillUsesLateSubmittedType() {
        val (_, submitted) = CyberTodoMapper.mapEvaluationsToTodos(subject, listOf(evaluation("지각 응시완료")))
        assertEquals(TodoType.SUBMITTED_LATE, submitted.single().type)
    }

    @Test
    fun refreshRemovesPreviouslyIncompleteQuizEvenWhenOtherCyberSubjectsFail() {
        val (oldTodos, _) = CyberTodoMapper.mapEvaluationsToTodos(subject, listOf(evaluation("미응시")))
        val (_, submitted) = CyberTodoMapper.mapEvaluationsToTodos(subject, listOf(evaluation("응시")))
        val result = toAppTodoData(
            subjects = emptyList(),
            previousData = TodoData(todos = oldTodos, subjects = listOf(subject)),
            loadedAt = "2026-10-05T00:00:00Z",
            cyberResult = CyberTodoResult(submitted = submitted, subjects = listOf(subject), isComplete = false),
            isCyberConnected = true,
        )
        assertTrue(result.todos.isEmpty())
        assertEquals(submitted.single().todoId, result.submitted.single().todoId)
    }

    private fun evaluation(status: String) = CyberEvaluation(
        type = CyberEvaluationType.QUIZ,
        typeCode = "02",
        typeName = "퀴즈",
        round = 1,
        week = "4주차",
        title = "퀴즈 4주차 본시험",
        startAt = "2026-09-28 21:57",
        endAt = "2026-10-05 23:59",
        timeLimit = "60분",
        applyText = "문제은행",
        submitStatus = status,
        ratio = "10%",
        isInPeriod = true,
        isResubmission = false,
        rawDeadlineFlag = "0",
        hasApplyButton = true,
    )
}
