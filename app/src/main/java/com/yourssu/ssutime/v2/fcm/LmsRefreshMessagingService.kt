package com.yourssu.ssutime.v2.fcm

import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourssu.data.TodoInfo
import com.yourssu.data.network.FcmRequest
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.getRemainingDays
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.notification.showCallAlert
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import com.yourssu.ssutime.v2.screen.main.RefreshSource
import com.yourssu.ssutime.v2.screen.main.TodoRefreshResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class LmsRefreshMessagingService : FirebaseMessagingService(), KoinComponent {
    private val lmsRefreshRepository: LmsRefreshRepository by inject()
    private val loginRepository: LoginRepository by inject()
    private val apiRepository: ApiRepository by inject()
    private val mainRepository: MainRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data[ACTION_KEY] == ACTION_CRAWL_LMS) {
            refreshTodos(message)
            return
        }

        showDeadlineCallAlert()
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed.")
        registerTokenIfLoggedIn(token)
    }

    private fun refreshTodos(message: RemoteMessage) {
        val requestId = message.data["requestId"]
            ?: message.messageId
            ?: Instant.now().toString()
        val refreshResult = runBlocking {
            lmsRefreshRepository.refreshTodos(
                source = RefreshSource.FCM,
                requestId = requestId,
                timeoutMillis = REFRESH_TIMEOUT_MILLIS,
            )
        }

        when (refreshResult) {
            is TodoRefreshResult.Success -> Log.i(TAG, "백그라운드 새로고침이 완료되었습니다.")
            is TodoRefreshResult.Failure -> Log.e(TAG, refreshResult.message, refreshResult.throwable)
            is TodoRefreshResult.Skipped -> Log.i(TAG, refreshResult.reason)
        }
    }

    private fun showDeadlineCallAlert() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val todo = runBlocking {
            mainRepository.getTodoData().todos.selectMostUrgentTodo()
        }
        if (todo == null) {
            Log.i(TAG, "전화 알림을 표시할 Todo가 없습니다.")
            return
        }

        showCallAlert(this, todo)
    }

    private fun List<TodoInfo>.selectMostUrgentTodo(): TodoInfo? = minWithOrNull(
        compareBy<TodoInfo> { getRemainingDays(it.due_date) }
            .thenBy { it.due_date },
    )

    private fun registerTokenIfLoggedIn(token: String) {
        serviceScope.launch {
            val loginData = loginRepository.getLoginData()
            val bearerToken = accessToken.ifBlank {
                loginData.accessToken.takeIf { loginData.isAutoLogin }.orEmpty()
            }
            if (bearerToken.isBlank()) {
                Log.i(TAG, "로그인 정보가 없어 FCM 토큰 등록을 건너뜁니다.")
                return@launch
            }

            accessToken = bearerToken
            runCatching {
                apiRepository.registerFCMToken(FcmRequest(token))
            }.onSuccess {
                Log.i(TAG, "FCM 토큰이 등록되었습니다.")
            }.onFailure { exception ->
                Log.e(TAG, "FCM 토큰 등록에 실패했습니다.", exception)
            }
        }
    }

    private companion object {
        private const val TAG = "LmsRefreshFCM"
        private const val ACTION_KEY = "action"
        private const val ACTION_CRAWL_LMS = "crawl_lms"
        private const val REFRESH_TIMEOUT_MILLIS = 30_000L
    }
}
