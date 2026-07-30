package com.yourssu.ssutime.desktop.core.login

import com.yourssu.ssutime.desktop.core.network.AuthTokenProvider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginServiceTest {
    @Test
    fun `LMS and backend login return a usable session`() = runBlocking {
        val lms = FakeLmsAuthenticator()
        val service = LoginService(
            lmsAuthenticator = lms,
            authApi = AuthTokenProvider { id, password ->
                assertEquals("20260000", id)
                assertEquals("password", password)
                "access-token"
            },
        )

        val session = service.login("20260000", "password")

        assertTrue(lms.didLogin)
        assertFalse(lms.didLogout)
        assertEquals("20260000", session.userId)
        assertEquals("access-token", session.accessToken)
    }

    @Test
    fun `backend token failure clears the LMS session`() {
        val lms = FakeLmsAuthenticator()
        val service = LoginService(
            lmsAuthenticator = lms,
            authApi = AuthTokenProvider { _, _ ->
                error("token request failed")
            },
        )

        assertFailsWith<IllegalStateException> {
            runBlocking {
                service.login("20260000", "password")
            }
        }
        assertTrue(lms.didLogin)
        assertTrue(lms.didLogout)
    }
}

private class FakeLmsAuthenticator : LmsAuthenticator {
    var didLogin = false
    var didLogout = false

    override suspend fun login(id: String, password: String) {
        didLogin = true
    }

    override suspend fun logout() {
        didLogout = true
    }
}
