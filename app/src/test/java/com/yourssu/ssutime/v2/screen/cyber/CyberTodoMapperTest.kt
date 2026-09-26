package com.yourssu.ssutime.v2.screen.cyber

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.isCyber
import com.yourssu.data.todoUniqueKey
import com.yourssu.ssutime.v2.screen.calendar.toLocalDate
import com.yourssu.ssutime.v2.todo.toTodoDeadlineInstantOrNull
import io.github.chlwhdtn03.data.Cyber.CyberLecture
import io.github.chlwhdtn03.data.Cyber.CyberSubject
import io.github.chlwhdtn03.data.Cyber.CyberWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CyberTodoMapperTest {

    @Test
    fun parseDeadlineIso_withDotFormat() {
        val result = CyberTodoMapper.parseDeadlineIso("2026.03.02 ~ 2026.03.15")
        assertEquals("2026-03-15T14:59:59Z", result)

        val instant = result.toTodoDeadlineInstantOrNull()
        assertNotNull(instant)
    }

    @Test
    fun parseDeadlineIso_withHyphenFormatAndTime() {
        val result = CyberTodoMapper.parseDeadlineIso("2026-03-02 09:00 ~ 2026-03-15 22:30:00")
        assertEquals("2026-03-15T13:30:00Z", result)

        val instant = result.toTodoDeadlineInstantOrNull()
        assertNotNull(instant)
    }

    @Test
    fun parseDeadlineIso_withDayOfWeekText() {
        val result = CyberTodoMapper.parseDeadlineIso("2026.03.02(월) ~ 2026.03.15(일)")
        assertEquals("2026-03-15T14:59:59Z", result)
    }

    @Test
    fun parseDeadlineIso_withMonthDayOnly() {
        val result = CyberTodoMapper.parseDeadlineIso("03.02 ~ 03.15", fallbackYear = 2026)
        assertEquals("2026-03-15T14:59:59Z", result)
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

    @Test
    fun mapToSubjectInfo_removesTrailingParenthesesWithText() {
        val subject = CyberSubject(
            name = "AI리터러시와 비판적 사고(2026-2 숭실대 학점교류)",
            category = "교양선택",
            professor = "김교수",
            credit = "3",
            year = "2026",
            semesterCode = "20",
            courseCode = "05083",
            deptCode = "001",
            userNo = "12345",
            progressPercent = 0,
        )
        val subjectInfo = CyberTodoMapper.mapToSubjectInfo(subject)
        assertEquals("AI리터러시와 비판적 사고", subjectInfo.name)
    }

    @Test
    fun cleanCyberSubjectName_removesVariousTrailingParentheses() {
        assertEquals("AI리터러시와 비판적 사고", CyberTodoMapper.cleanCyberSubjectName("AI리터러시와 비판적 사고(2026-2 숭실대 학점교류)"))
        assertEquals("AI리터러시와 비판적 사고", CyberTodoMapper.cleanCyberSubjectName("AI리터러시와 비판적 사고 (2026-2 숭실대 학점교류)"))
        assertEquals("기초 프로그래밍", CyberTodoMapper.cleanCyberSubjectName("기초 프로그래밍 (학점교류)"))
        assertEquals("운영체제", CyberTodoMapper.cleanCyberSubjectName("운영체제(2026-1)"))
        assertEquals("인공지능개론", CyberTodoMapper.cleanCyberSubjectName("인공지능개론"))
    }

    @Test
    fun isCyber_returnsFalseForRegularLmsLectureWithMinusOneTodoId() {
        val lmsLecture = TodoInfo(
            todoId = -1,
            title = "1주차 동영상 강의",
            due_date = "2026-03-15T14:59:59Z",
            type = TodoType.COMMONS,
            subject = SubjectInfo(id = 501, name = "운영체제", professor = "교수님"),
            url = "https://canvas.ssu.ac.kr/courses/501/modules/items/888",
        )
        assertFalse(lmsLecture.isCyber())
    }

    @Test
    fun isCyber_returnsTrueForCyberTodo() {
        val cyberTodo = TodoInfo(
            todoId = -10001,
            title = "1주차 1강",
            due_date = "2026-03-15T14:59:59Z",
            type = TodoType.COMMONS,
            subject = SubjectInfo(id = -100001, name = "AI리터러시", professor = "교수님"),
            url = "https://lms.kcu.ac/atnlcSubj/atnlcApe/list",
        )
        assertTrue(cyberTodo.isCyber())
    }
}
