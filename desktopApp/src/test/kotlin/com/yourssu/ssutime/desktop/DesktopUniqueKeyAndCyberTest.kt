package com.yourssu.ssutime.desktop

import com.yourssu.data.DiscussionInfo
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.isCyber
import com.yourssu.ssutime.desktop.core.cyber.CyberTodoMapper
import com.yourssu.ssutime.desktop.core.model.desktopItemKey
import io.github.chlwhdtn03.data.Cyber.CyberSubject
import io.github.chlwhdtn03.data.Cyber.CyberWeek
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopUniqueKeyAndCyberTest {

    @Test
    fun testDesktopItemKeyProducesUniqueKeys() {
        val subject = SubjectInfo(
            id = 100,
            name = "운영체제",
            professor = "교수",
        )
        val todo1 = TodoInfo(
            todoId = 0,
            title = "강의",
            due_date = "2026-03-08T23:59:59+09:00",
            type = TodoType.COMMONS,
            subject = subject,
        )
        val todo2 = TodoInfo(
            todoId = 0,
            title = "강의",
            due_date = "2026-03-08T23:59:59+09:00",
            type = TodoType.COMMONS,
            subject = subject,
        )

        val key0 = todo1.desktopItemKey(0)
        val key1 = todo2.desktopItemKey(1)

        assertNotEquals(key0, key1)
        assertTrue(key0.endsWith(":0"))
        assertTrue(key1.endsWith(":1"))
    }

    @Test
    fun testSubjectInfoEqualityDetectsDiscussionReadStateChanges() {
        val discussionUnread = DiscussionInfo(
            id = 42,
            title = "공지사항",
            readState = "unread",
        )
        val subjectOriginal = SubjectInfo(
            id = 1234,
            name = "운영체제",
            professor = "교수님",
            discussions = listOf(discussionUnread),
        )

        val discussionRead = discussionUnread.copy(readState = "read")
        val subjectUpdated = subjectOriginal.copy(discussions = listOf(discussionRead))

        assertNotEquals(subjectOriginal, subjectUpdated)
    }

    @Test
    fun testDesktopSessionStoreStoresCyberCredentials() {
        val directory = Files.createTempDirectory("ssutime-desktop-cyber")
        try {
            val store = DesktopSessionStore(directory)
            var state = store.save(
                current = DesktopStoredState(onboardingCompleted = true),
                userId = "student1",
                password = "pw1",
                autoLogin = true,
            )

            state = store.saveCyber(state, cyberUserId = "cyber_user", cyberPassword = "cyber_password")
            state = store.update(state.copy(isEnableSubmittedFile = true))

            val reloaded = DesktopSessionStore(directory).load()
            val cyberCreds = store.cyberCredentials(reloaded)

            assertTrue(reloaded.isCyberConnected)
            assertEquals("cyber_user", reloaded.cyberUserId)
            assertEquals("cyber_password", cyberCreds?.password)
            assertTrue(reloaded.isEnableSubmittedFile)

            val cleared = store.clearCyber(reloaded)
            assertFalse(cleared.isCyberConnected)
            assertEquals("", cleared.cyberUserId)
            assertNull(store.cyberCredentials(cleared))
        } finally {
            directory.resolve("state.json").deleteIfExists()
            directory.resolve("secret.key").deleteIfExists()
            directory.resolve("state.json.tmp").deleteIfExists()
            directory.deleteIfExists()
        }
    }

    @Test
    fun testCyberTodoMapperMapsCorrectly() {
        val cyberSubject = CyberSubject(
            name = "컴퓨터네트워크",
            category = "전선",
            professor = "교수님",
            credit = "3",
            year = "2026",
            semesterCode = "10",
            courseCode = "CS101",
            deptCode = "01",
            userNo = "12345",
            progressPercent = 50,
        )

        val cyberWeek = CyberWeek(
            weekNo = 1,
            attendancePeriod = "2026-03-02 00:00:00 ~ 2026-03-08 23:59:59",
            topic = "1주차 강의",
            attendanceStatus = "미출석",
            lectures = emptyList(),
        )

        val subjectInfo = CyberTodoMapper.mapToSubjectInfo(cyberSubject)
        assertEquals("컴퓨터네트워크", subjectInfo.name)
        assertTrue(subjectInfo.id < 0)

        val (todos, submitted) = CyberTodoMapper.mapWeeksToTodos(cyberSubject, subjectInfo, listOf(cyberWeek))
        assertEquals(1, todos.size)
        assertEquals(0, submitted.size)

        val todoInfo = todos.first()
        assertTrue(todoInfo.isCyber())
        assertEquals(TodoType.COMMONS, todoInfo.type)
        assertTrue(todoInfo.title.contains("1주차"))
        assertTrue(todoInfo.due_date.contains("2026-03-08"))
    }
}
