package com.yourssu.ssutime.v2.screen.main

import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectNameFormattingTest {
    @Test
    fun `removes a trailing numeric course number including its parentheses`() {
        assertEquals("운영체제", "운영체제 (20202222)".withoutTrailingCourseNumber())
        assertEquals("자료구조", "자료구조(200000)".withoutTrailingCourseNumber())
    }

    @Test
    fun `keeps non-trailing or non-numeric parentheses`() {
        assertEquals("AI (기초)", "AI (기초)".withoutTrailingCourseNumber())
        assertEquals("수학 (2024) 심화", "수학 (2024) 심화".withoutTrailingCourseNumber())
    }
}
