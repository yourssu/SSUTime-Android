package com.yourssu.ssutime.v2.fcm

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

private const val TAG = "LmsRefreshFCM"
private const val ACTION_CRAWL_LMS = "crawl_lms"
private const val TYPE_LMS_REFRESH = "lms_refresh"
private const val TYPE_DEADLINE_CALL_ALERT = "deadline_call_alert"
private const val TYPE_CALL_ALERT = "call_alert"
private const val HANDLE_LMS_REFRESH = "lms_refresh"
private const val HANDLE_CALL_ALERT = "deadline_call_alert"
private const val HANDLE_UNKNOWN = "unknown"
private const val REFRESH_TIMEOUT_MILLIS = 30_000L

class LmsRefreshMessagingService : FirebaseMessagingService(), KoinComponent {
    private val lmsRefreshRepository: LmsRefreshRepository by inject()
    private val loginRepository: LoginRepository by inject()
    private val apiRepository: ApiRepository by inject()
    private val mainRepository: MainRepository by inject()
    private val fcmDebugHistoryRepository: FcmDebugHistoryRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val messageType = message.toLmsMessageType()
        val handledAs = messageType.debugName
        val debugRecordId = runBlocking {
            fcmDebugHistoryRepository.recordReceived(message, handledAs)
        }

        val result = when (messageType) {
            LmsFcmMessageType.LmsRefresh -> refreshTodos(message).toDebugHandleResult()
            LmsFcmMessageType.DeadlineCallAlert -> showDeadlineCallAlert()
            LmsFcmMessageType.Unknown -> {
                val detail = "처리하지 않는 FCM입니다. data=${message.data}"
                Log.i(TAG, detail)
                FcmDebugHandleResult("skipped", detail)
            }
        }

        runBlocking {
            fcmDebugHistoryRepository.markHandled(
                recordId = debugRecordId,
                status = result.status,
                detail = result.detail,
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed.")
        registerTokenIfLoggedIn(token)
    }

    private fun refreshTodos(message: RemoteMessage): TodoRefreshResult {
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
        return refreshResult
    }

    private fun showDeadlineCallAlert(): FcmDebugHandleResult {
        val todo = runBlocking {
            mainRepository.getTodoData().todos.selectMostUrgentTodo()
        }
        if (todo == null) {
            val detail = "전화 알림을 표시할 Todo가 없습니다."
            Log.i(TAG, detail)
            return FcmDebugHandleResult("skipped", detail)
        }

        showCallAlert(this, todo)
        return FcmDebugHandleResult("success", "전화 알림 표시: ${todo.title}")
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

}

private enum class LmsFcmMessageType(
    val debugName: String,
) {
    LmsRefresh(HANDLE_LMS_REFRESH),
    DeadlineCallAlert(HANDLE_CALL_ALERT),
    Unknown(HANDLE_UNKNOWN),
}

private fun RemoteMessage.toLmsMessageType(): LmsFcmMessageType {
    val action = data["action"]
    val type = data["type"]

    return when {
        type == TYPE_LMS_REFRESH || action == TYPE_LMS_REFRESH || action == ACTION_CRAWL_LMS ->
            LmsFcmMessageType.LmsRefresh

        type == TYPE_DEADLINE_CALL_ALERT ||
            action == TYPE_DEADLINE_CALL_ALERT ||
            type == TYPE_CALL_ALERT ||
            action == TYPE_CALL_ALERT ->
            LmsFcmMessageType.DeadlineCallAlert

        else -> LmsFcmMessageType.Unknown
    }
}

private data class FcmDebugHandleResult(
    val status: String,
    val detail: String,
)

private fun TodoRefreshResult.toDebugHandleResult(): FcmDebugHandleResult =
    when (this) {
        is TodoRefreshResult.Success -> FcmDebugHandleResult(
            status = "success",
            detail = "백그라운드 새로고침 완료: todos=${summary.todoCount}, submitted=${summary.submittedCount}, semesters=${summary.semesterCount}",
        )

        is TodoRefreshResult.Failure -> FcmDebugHandleResult(
            status = "failure",
            detail = message,
        )

        is TodoRefreshResult.Skipped -> FcmDebugHandleResult(
            status = "skipped",
            detail = reason,
        )
    }
