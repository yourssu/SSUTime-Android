package com.yourssu.ssutime.v2.screen.notice

import com.yourssu.data.DiscussionInfo
import com.yourssu.data.SubjectInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class NoticeScreenSubjectSelectionTest {

    private fun createSubject(id: Int, name: String, unreadCount: Int, readCount: Int): SubjectInfo {
        val discussions = mutableListOf<DiscussionInfo>()
        for (i in 0 until unreadCount) {
            discussions.add(
                DiscussionInfo(
                    id = id * 100 + i,
                    title = "Unread $i",
                    message = "Message",
                    url = "https://smartid.ssu.ac.kr",
                    readState = "unread",
                    createdAt = "2026-04-27T17:08:00Z"
                )
            )
        }
        for (i in 0 until readCount) {
            discussions.add(
                DiscussionInfo(
                    id = id * 100 + unreadCount + i,
                    title = "Read $i",
                    message = "Message",
                    url = "https://smartid.ssu.ac.kr",
                    readState = "read",
                    createdAt = "2026-04-27T17:08:00Z"
                )
            )
        }
        return SubjectInfo(
            id = id,
            name = name,
            professor = "교수",
            discussions = discussions
        )
    }

    @Test
    fun findFirstUnreadSubjectIndex_whenFirstSubjectHasUnread_returnsZero() {
        val subjects = listOf(
            createSubject(1, "과목1", unreadCount = 1, readCount = 0),
            createSubject(2, "과목2", unreadCount = 2, readCount = 1),
            createSubject(3, "과목3", unreadCount = 0, readCount = 3),
        )

        assertEquals(0, subjects.findFirstUnreadSubjectIndex())
    }

    @Test
    fun findFirstUnreadSubjectIndex_whenMiddleSubjectHasFirstUnread_returnsMiddleIndex() {
        val subjects = listOf(
            createSubject(1, "과목1", unreadCount = 0, readCount = 2),
            createSubject(2, "과목2", unreadCount = 1, readCount = 0),
            createSubject(3, "과목3", unreadCount = 2, readCount = 1),
        )

        assertEquals(1, subjects.findFirstUnreadSubjectIndex())
    }

    @Test
    fun findFirstUnreadSubjectIndex_whenNoSubjectHasUnread_returnsMinusOne() {
        val subjects = listOf(
            createSubject(1, "과목1", unreadCount = 0, readCount = 1),
            createSubject(2, "과목2", unreadCount = 0, readCount = 2),
        )

        assertEquals(-1, subjects.findFirstUnreadSubjectIndex())
    }

    @Test
    fun findFirstUnreadSubjectIndex_whenListIsEmpty_returnsMinusOne() {
        val subjects = emptyList<SubjectInfo>()
        assertEquals(-1, subjects.findFirstUnreadSubjectIndex())
    }
}
