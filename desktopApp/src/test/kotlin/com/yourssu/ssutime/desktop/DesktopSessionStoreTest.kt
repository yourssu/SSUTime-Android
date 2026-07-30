package com.yourssu.ssutime.desktop

import com.yourssu.ssutime.desktop.core.model.AppTodoData
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopSessionStoreTest {
    @Test
    fun `auto login credentials and cache survive reload`() {
        val directory = Files.createTempDirectory("ssutime-desktop-store")
        try {
            val store = DesktopSessionStore(directory)
            var state = store.save(
                current = DesktopStoredState(onboardingCompleted = true),
                userId = "student",
                password = "secret",
                autoLogin = true,
            )
            state = store.update(
                state.copy(todoData = AppTodoData(loadedAt = "2026-07-30T00:00:00Z")),
            )

            val reloaded = DesktopSessionStore(directory).load()
            val credentials = store.credentials(reloaded)

            assertEquals("student", credentials?.userId)
            assertEquals("secret", credentials?.password)
            assertTrue(reloaded.onboardingCompleted)
            assertEquals("2026-07-30T00:00:00Z", reloaded.todoData.loadedAt)
        } finally {
            directory.resolve("state.json").deleteIfExists()
            directory.resolve("secret.key").deleteIfExists()
            directory.resolve("state.json.tmp").deleteIfExists()
            directory.deleteIfExists()
        }
    }

    @Test
    fun `logout clears session data but keeps app preferences`() {
        val directory = Files.createTempDirectory("ssutime-desktop-logout")
        try {
            val store = DesktopSessionStore(directory)
            val saved = store.save(
                current = DesktopStoredState(
                    onboardingCompleted = true,
                    systemNotificationsEnabled = true,
                    todoData = AppTodoData(loadedAt = "cached"),
                ),
                userId = "student",
                password = "secret",
                autoLogin = true,
            )

            val loggedOut = store.logout(saved)

            assertFalse(loggedOut.autoLogin)
            assertNull(store.credentials(loggedOut))
            assertEquals("", loggedOut.todoData.loadedAt)
            assertTrue(loggedOut.onboardingCompleted)
            assertTrue(loggedOut.systemNotificationsEnabled)
        } finally {
            directory.resolve("state.json").deleteIfExists()
            directory.resolve("secret.key").deleteIfExists()
            directory.resolve("state.json.tmp").deleteIfExists()
            directory.deleteIfExists()
        }
    }
}
