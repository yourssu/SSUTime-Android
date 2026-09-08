package com.yourssu.ssutime.v2.screen.cyber

import com.yourssu.data.TodoType
import com.yourssu.data.todoUniqueKey
import com.yourssu.ssutime.v2.screen.calendar.toLocalDate
import com.yourssu.ssutime.v2.todo.toTodoDeadlineInstantOrNull
import io.github.chlwhdtn03.data.Cyber.CyberLecture
import io.github.chlwhdtn03.data.Cyber.CyberSubject
import io.github.chlwhdtn03.data.Cyber.CyberWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CyberTodoMapperTest {

    @Test
    fun parseDeadlineIso_withDotFormat() {
        val result = CyberTodoMapper.parseDeadlineIso("2026.03.02 ~ 2026.03.15")
        assertEquals("2026-03-15T23:59:59+09:00", result)

        val instant = result.toTodoDeadlineInstantOrNull()
        assertNotNull(instant)
    }

    @Test
    fun parseDeadlineIso_withHyphenFormatAndTime() {
        val result = CyberTodoMapper.parseDeadlineIso("2026-03-02 09:00 ~ 2026-03-15 22:30:00")
        assertEquals("2026-03-15T22:30:00+09:00", result)

        val instant = result.toTodoDeadlineInstantOrNull()
        assertNotNull(instant)
    }

    @Test
    fun parseDeadlineIso_withDayOfWeekText() {
        val result = CyberTodoMapper.parseDeadlineIso("2026.03.02(월) ~ 2026.03.15(일)")
        assertEquals("2026-03-15T23:59:59+09:00", result)
    }

    @Test
    fun parseDeadlineIso_withMonthDayOnly() {
        val result = CyberTodoMapper.parseDeadlineIso("03.02 ~ 03.15", fallbackYear = 2026)
        assertEquals("2026-03-15T23:59:59+09:00", result)
    }

    @Test
    fun parseDurationSeconds_variousFormats() {
        assertEquals(1530.0, CyberTodoMapper.parseDurationSeconds("25:30"), 0.001)
        assertEquals(3600.0, CyberTodoMapper.parseDurationSeconds("01:00:00"), 0.001)
        assertEquals(-1.0, CyberTodoMapper.parseDurationSeconds(""), 0.001)
    }

    @Test
    fun mapWeeksToTodos_correctClassificationAndIds() {
        val subject = CyberSubject(
            name = "인공지능개론",
            category = "전공선택",
            professor = "홍길동",
            credit = "3",
            year = "2026",
            semesterCode = "10",
            courseCode = "05082",
            deptCode = "001",
            userNo = "12345",
            progressPercent = 50,
        )
        val subjectInfo = CyberTodoMapper.mapToSubjectInfo(subject)

        // 과목 ID는 음수여야 일반 LMS와 충돌하지 않음
        assertTrue(subjectInfo.id < 0)
        assertEquals("인공지능개론", subjectInfo.name)
        assertEquals("홍길동", subjectInfo.professor)

        val weeks = listOf(
            // 1주차: 완료된 강의 (출석 완료)
            CyberWeek(
                weekNo = 1,
                attendancePeriod = "2026.03.02 ~ 2026.03.08",
                topic = "오리엔테이션",
                attendanceStatus = "출석",
                lectures = listOf(
                    CyberLecture(
                        lectureNo = 1,
                        statusText = "학습완료",
                        progressPercent = 100,
                        studyTime = "30:00",
                        baseTime = "25:00",
                        videoFilePath = null,
                        audioFilePath = null,
                    )
                ),
            ),
            // 2주차: 미완료 강의 (출석 진행중)
            CyberWeek(
                weekNo = 2,
                attendancePeriod = "2026.03.09 ~ 2026.03.15",
                topic = "머신러닝 기초",
                attendanceStatus = "미출석",
                lectures = listOf(
                    CyberLecture(
                        lectureNo = 1,
                        statusText = "미학습",
                        progressPercent = 30,
                        studyTime = "10:00",
                        baseTime = "30:00",
                        videoFilePath = null,
                        audioFilePath = null,
                    ),
                    CyberLecture(
                        lectureNo = 2,
                        statusText = "학습완료",
                        progressPercent = 100,
                        studyTime = "30:00",
                        baseTime = "30:00",
                        videoFilePath = null,
                        audioFilePath = null,
                    ),
                ),
            ),
            // 3주차: 결석 처리된 주차
            CyberWeek(
                weekNo = 3,
                attendancePeriod = "2026.03.16 ~ 2026.03.22",
                topic = "딥러닝 개요",
                attendanceStatus = "결석",
                lectures = listOf(
                    CyberLecture(
                        lectureNo = 1,
                        statusText = "미학습",
                        progressPercent = 0,
                        studyTime = "00:00",
                        baseTime = "25:00",
                        videoFilePath = null,
                        audioFilePath = null,
                    )
                ),
            ),
        )

        val (todos, submitted) = CyberTodoMapper.mapWeeksToTodos(subject, subjectInfo, weeks)

        // 2주차 1강만 미완료 todos에 포함되어야 함
        assertEquals(1, todos.size)
        val urgentTodo = todos[0]
        assertEquals("2주차 1강 - 머신러닝 기초", urgentTodo.title)
        assertEquals(TodoType.COMMONS, urgentTodo.type)
        assertEquals(LocalDate.of(2026, 3, 15), urgentTodo.toLocalDate())
        assertTrue(urgentTodo.todoId < 0)
        assertNotNull(urgentTodo.todoUniqueKey())

        // 1주차 1강(출석), 2주차 2강(100%), 3주차 1강(결석)은 submitted에 포함
        assertEquals(3, submitted.size)
    }
}
