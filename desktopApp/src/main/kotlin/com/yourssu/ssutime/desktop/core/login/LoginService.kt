package com.yourssu.ssutime.desktop.core.login

import com.yourssu.ssutime.desktop.core.network.AuthTokenProvider
import com.yourssu.ssutime.desktop.lms.loginLms
import com.yourssu.ssutime.desktop.lms.logoutLms
import kotlin.coroutines.cancellation.CancellationException

data class LoginSession(
    val userId: String,
    val accessToken: String,
)

interface LmsAuthenticator {
    suspend fun login(id: String, password: String)
    suspend fun logout()
}

class LmsApiAuthenticator : LmsAuthenticator {
    override suspend fun login(id: String, password: String) {
        loginLms(id, password)
    }

    override suspend fun logout() {
        logoutLms()
    }
}

class LoginService(
    private val lmsAuthenticator: LmsAuthenticator,
    private val authApi: AuthTokenProvider,
) {
    suspend fun login(id: String, password: String): LoginSession {
        require(id.isNotBlank() && password.isNotBlank()) {
            "아이디와 비밀번호를 모두 입력해주세요."
        }

        lmsAuthenticator.login(id, password)

        val accessToken = try {
            authApi.requestAccessToken(id, password)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            runCatching { lmsAuthenticator.logout() }
            throw exception
        }

        return LoginSession(
            userId = id,
            accessToken = accessToken,
        )
    }
}
