package com.yourssu.ssutime.v2.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.network.FcmRequest
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.notification.showCallAlert
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import com.yourssu.ssutime.v2.screen.main.RefreshSource
import com.yourssu.ssutime.v2.screen.main.TodoRefreshResult
import com.yourssu.ssutime.v2.screen.main.sortedForMainDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

private const val TAG = "LmsRefreshFCM"
private const val ACTION_CRAWL_LMS = "crawl_lms"
private const val REFRESH_TIMEOUT_MILLIS = 30_000L

class LmsRefreshMessagingService : FirebaseMessagingService(), KoinComponent {
    private val lmsRefreshRepository: LmsRefreshRepository by inject()
    private val loginRepository: LoginRepository by inject()
    private val apiRepository: ApiRepository by inject()
    private val mainRepository: MainRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        runCatching {
            handleBackendMessage(message)
        }.onFailure { exception ->
            SentryExceptionReporter.capture(exception)
            val detail = "FCM 처리 중 예외가 발생했습니다: ${exception.message ?: exception::class.java.simpleName}"
            Log.e(TAG, detail, exception)
        }
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, "FCM token refreshed.")
        registerTokenIfLoggedIn(token)
    }

    private fun handleBackendMessage(message: RemoteMessage) {
        val action = message.data["action"].orEmpty()
        if (action == ACTION_CRAWL_LMS) {
            refreshTodos(message)
            return
        }

        if (message.notification != null || message.data.isNotEmpty()) {
            showDeadlineCallAlert(message)
            return
        }

        val actualAction = action.ifBlank { "-" }
        val detail = "지원하지 않는 FCM action입니다. expected=$ACTION_CRAWL_LMS, actual=$actualAction"
        Log.i(TAG, "$detail, data=${message.data}")
    }

    private fun refreshTodos(message: RemoteMessage) {
        val requestId = message.messageId
            ?: message.sentTime.takeIf { it > 0L }?.let { "fcm-$it" }
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

    private fun registerTokenIfLoggedIn(token: String) {
        serviceScope.launch {
            val loginData = loginRepository.getLoginData()
            val bearerToken = accessToken.ifBlank { loginData.accessToken }
            if (bearerToken.isBlank()) {
                Log.i(TAG, "로그인 정보가 없어 FCM 토큰 등록을 건너뜁니다.")
                return@launch
            }

            accessToken = bearerToken
            runCatching {
                apiRepository.registerFCMToken(FcmRequest(token))
            }.onSuccess { status ->
                Log.i(TAG, "FCM 토큰이 등록되었습니다. status=$status")
            }.onFailure { exception ->
                SentryExceptionReporter.capture(exception)
                Log.e(TAG, "FCM 토큰 등록에 실패했습니다.", exception)
            }
        }
    }

    private fun showDeadlineCallAlert(message: RemoteMessage) {
        val alertData = runBlocking { mainRepository.getAlertData() }
        if (!alertData.allowCallAlert) {
            Log.i(TAG, "전화 알림 설정이 꺼져 있어 FCM 마감 임박 전화 알림을 표시하지 않습니다.")
            return
        }

        val todo = runBlocking {
            mainRepository.getTodoData().todos.selectMostUrgentTodo()
        } ?: message.toFallbackTodoInfo()

        if (todo == null) {
            Log.i(TAG, "전화 알림을 표시할 Todo가 없습니다. data=${message.data}")
            return
        }

        showCallAlert(this, todo)
        Log.i(TAG, "FCM 마감 임박 전화 알림을 표시했습니다. todo=${todo.title}")
    }
}

private fun List<TodoInfo>.selectMostUrgentTodo(): TodoInfo? =
    sortedForMainDisplay().firstOrNull()

private fun RemoteMessage.toFallbackTodoInfo(): TodoInfo? {
    val title = data["todoTitle"]
        ?: data["title"]
        ?: notification?.title
        ?: return null
    val subjectName = data["subjectName"]
        ?: data["subject"]
        ?: data["courseName"]
        ?: ""
    val professor = data["professor"].orEmpty()
    val subjectId = data["subjectId"]?.toIntOrNull() ?: 0
    val subject = SubjectInfo(
        id = subjectId,
        name = subjectName,
        professor = professor,
    ).takeIf { it.name.isNotBlank() || it.professor.isNotBlank() }

    return TodoInfo(
        todoId = data["todoId"]?.toIntOrNull()
            ?: messageId?.hashCode()
            ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
        title = title,
        due_date = data["dueDate"]
            ?: data["due_date"]
            ?: data["deadline"]
            ?: "",
        type = data["todoType"].toTodoType(),
        subject = subject,
    )
}

private fun String?.toTodoType(): TodoType =
    TodoType.values().firstOrNull { type ->
        equals(type.name, ignoreCase = true) || this == type.kor
    } ?: TodoType.ASSIGNMENT
