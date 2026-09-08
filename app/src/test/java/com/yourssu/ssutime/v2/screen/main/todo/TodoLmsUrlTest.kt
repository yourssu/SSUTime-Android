package com.yourssu.ssutime.v2.screen.main.todo

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoLmsUrlTest {

    @Test
    fun `quiz type generates canvas quizzes url using componentId`() {
        val todo = TodoInfo(
            todoId = 1001,
            componentId = 777,
            moduleItemId = 888,
            title = "중간 퀴즈",
            due_date = "2026-08-23T23:59:59Z",
            type = TodoType.QUIZ,
            subject = SubjectInfo(id = 501, name = "운영체제", professor = "교수님")
        )

        assertEquals("https://canvas.ssu.ac.kr/courses/501/quizzes/777", todo.toLmsUrl())
    }

    @Test
    fun `assignment type generates canvas assignments url using todoId`() {
        val todo = TodoInfo(
            todoId = 2002,
            componentId = 777,
            moduleItemId = 888,
            title = "과제 1",
            due_date = "2026-08-23T23:59:59Z",
            type = TodoType.ASSIGNMENT,
            subject = SubjectInfo(id = 502, name = "자료구조", professor = "교수님")
        )

        assertEquals("https://canvas.ssu.ac.kr/courses/502/assignments/2002", todo.toLmsUrl())
    }

    @Test
    fun `commons lecture type generates canvas modules items url using moduleItemId`() {
        val todo = TodoInfo(
            todoId = 3003,
            componentId = 777,
            moduleItemId = 888,
            title = "1주차 동영상 강의",
            due_date = "2026-08-23T23:59:59Z",
            type = TodoType.COMMONS,
            subject = SubjectInfo(id = 503, name = "컴퓨터구조", professor = "교수님")
        )

        assertEquals("https://canvas.ssu.ac.kr/courses/503/modules/items/888", todo.toLmsUrl())
    }

    @Test
    fun `cyber todo returns url property directly instead of canvas url`() {
        val cyberUrl = "https://lms.kcu.ac/atnlcSubj/atnlcApe/list"
        val todo = TodoInfo(
            todoId = -1001,
            title = "1주차 1강",
            due_date = "2026-03-15T23:59:59+09:00",
            type = TodoType.COMMONS,
            subject = SubjectInfo(id = -2001, name = "사이버대학교 강의", professor = "교수님"),
            url = cyberUrl,
        )

        assertEquals(cyberUrl, todo.toLmsUrl())
    }

    @Test
    fun `cyber todo with blank url falls back to cyber portal base url`() {
        val todo = TodoInfo(
            todoId = -1002,
            title = "2주차 1강",
            due_date = "2026-03-22T23:59:59+09:00",
            type = TodoType.COMMONS,
            subject = SubjectInfo(id = -2002, name = "사이버대학교 강의2", professor = "교수님"),
            url = "",
        )

        assertEquals("https://lms.kcu.ac", todo.toLmsUrl())
    }


    @Test
    fun `uses fallback subjectId when subject is null`() {
        val todo = TodoInfo(
            todoId = 4004,
            componentId = 999,
            moduleItemId = 888,
            title = "퀴즈",
            due_date = "2026-08-23T23:59:59Z",
            type = TodoType.QUIZ,
            subject = null
        ).apply {
            subjectId = 504
        }

        assertEquals("https://canvas.ssu.ac.kr/courses/504/quizzes/999", todo.toLmsUrl())
    }

    @Test
    fun `formatVideoDuration formats seconds into hours minutes and seconds correctly`() {
        assertEquals("45초", formatVideoDuration(45.0))
        assertEquals("25분", formatVideoDuration(1500.0))
        assertEquals("25분 30초", formatVideoDuration(1530.0))
        assertEquals("1시간", formatVideoDuration(3600.0))
        assertEquals("1시간 1분", formatVideoDuration(3660.0))
        assertEquals("1시간 1분 5초", formatVideoDuration(3665.0))
        assertEquals("", formatVideoDuration(0.0))
        assertEquals("", formatVideoDuration(-1.0))
    }

    @Test
    fun `parseHtmlToAnnotatedString handles blank string`() {
        assertEquals("", parseHtmlToAnnotatedString("").text)
        assertEquals("", parseHtmlToAnnotatedString("   ").text)
    }

    @Test
    fun `parseHtmlToAnnotatedString falls back safely without crashing on JVM unit test`() {
        val html = "<p>과제 설명입니다. <b>중요</b> 안내</p>"
        val result = parseHtmlToAnnotatedString(html)
        // On JVM unit tests without android.jar framework mocks, runCatching catches the stub exception and returns the string safely
        assertEquals(html, result.text)
    }

    @Test
    fun `parseHtmlToAnnotatedString handles plain text without html`() {
        val plain = "일반 텍스트입니다."
        val result = parseHtmlToAnnotatedString(plain)
        assertEquals(plain, result.text)
    }
}
