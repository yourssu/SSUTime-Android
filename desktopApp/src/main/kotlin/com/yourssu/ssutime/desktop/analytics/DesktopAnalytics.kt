package com.yourssu.ssutime.desktop.analytics

import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.desktop.DesktopBuildConfig
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import com.yourssu.ssutime.desktop.core.network.createSsuTimeHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Serializable
internal data class PostHogBatchRequest(
    @SerialName("api_key") val apiKey: String,
    @SerialName("batch") val batch: List<PostHogEventPayload>,
)

@Serializable
internal data class PostHogEventPayload(
    val event: String,
    @SerialName("distinct_id") val distinctId: String,
    val properties: Map<String, JsonElement> = emptyMap(),
    val timestamp: String? = null,
)

object DesktopAnalytics {
    private const val POSTHOG_HOST = "https://us.i.posthog.com"
    private val httpClient: HttpClient by lazy { createSsuTimeHttpClient() }

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val identifiedDistinctId = AtomicReference<String?>(null)
    private val anonymousDistinctId = AtomicReference<String>(UUID.randomUUID().toString())
    private val eventQueue = ConcurrentLinkedQueue<PostHogEventPayload>()
    private val isFlushing = AtomicBoolean(false)
    private var flushLoopJob: Job? = null

    init {
        startFlushLoop()
    }

    fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        applicationScope.launch(block = block)

    fun initializeAnonymousId(savedAnonymousId: String?): String {
        val id = savedAnonymousId?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()
        anonymousDistinctId.set(id)
        return id
    }

    fun currentAnonymousId(): String = anonymousDistinctId.get()

    fun flush() {
        applicationScope.launch {
            flushInternal()
        }
    }

    private fun startFlushLoop() {
        flushLoopJob?.cancel()
        flushLoopJob = applicationScope.launch {
            while (isActive) {
                delay(5_000L)
                if (!eventQueue.isEmpty()) {
                    flushInternal()
                }
            }
        }
    }

    private suspend fun flushInternal() {
        val apiKey = DesktopBuildConfig.POSTHOG_API_KEY.trim()
        if (apiKey.isBlank() || eventQueue.isEmpty()) return

        if (!isFlushing.compareAndSet(false, true)) return
        try {
            val batch = mutableListOf<PostHogEventPayload>()
            while (batch.size < 50) {
                val item = eventQueue.poll() ?: break
                batch.add(item)
            }
            if (batch.isEmpty()) return

            val request = PostHogBatchRequest(
                apiKey = apiKey,
                batch = batch,
            )

            val response = runCatching {
                httpClient.post("$POSTHOG_HOST/batch/") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }.getOrNull()

            if (response == null || !response.status.isSuccess()) {
                // Re-queue items if sending failed
                for (item in batch.reversed()) {
                    eventQueue.add(item)
                }
            }
        } finally {
            isFlushing.set(false)
        }
    }

    private fun capture(
        event: String,
        properties: Map<String, Any?> = emptyMap(),
        flushImmediately: Boolean = false,
    ) {
        val apiKey = DesktopBuildConfig.POSTHOG_API_KEY.trim()
        if (apiKey.isBlank()) return

        val distinctId = identifiedDistinctId.get() ?: anonymousDistinctId.get()

        val fullProps = mutableMapOf<String, Any?>(
            "\$lib" to "posthog-desktop",
            "\$lib_version" to "1.0.0",
            "\$os" to "Windows",
            "\$os_version" to (System.getProperty("os.version") ?: "10.0"),
            "\$device_type" to "Desktop",
            "platform" to "windows",
            "\$app_version" to DesktopBuildConfig.VERSION_NAME,
        )
        fullProps.putAll(properties)

        val jsonProperties = fullProps.mapValues { (_, value) ->
            when (value) {
                null -> JsonNull
                is Boolean -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is JsonElement -> value
                else -> JsonPrimitive(value.toString())
            }
        }

        val payload = PostHogEventPayload(
            event = event,
            distinctId = distinctId,
            properties = jsonProperties,
            timestamp = Instant.now().toString(),
        )

        eventQueue.add(payload)

        if (flushImmediately || eventQueue.size >= 10) {
            flush()
        }
    }

    // --- Authentication & User Identity ---

    suspend fun identifyUser(loginId: String) {
        withContext(applicationScope.coroutineContext) {
            val distinctId = postHogDistinctId(loginId) ?: return@withContext
            if (identifiedDistinctId.get() == distinctId) return@withContext

            val prevAnonId = anonymousDistinctId.get()
            capture(
                event = "\$identify",
                properties = mapOf(
                    "\$anon_distinct_id" to prevAnonId,
                ),
                flushImmediately = true,
            )
            identifiedDistinctId.set(distinctId)
            flush()
        }
    }

    fun resetUser(): String {
        identifiedDistinctId.set(null)
        val newAnonId = UUID.randomUUID().toString()
        anonymousDistinctId.set(newAnonId)
        flush()
        return newAnonId
    }

    fun postHogDistinctId(loginId: String): String? = loginId.toHashedDistinctId()

    fun currentPostHogDistinctId(): String? = identifiedDistinctId.get()

    // --- Event Capture APIs (1:1 matching Android) ---

    fun viewLogin(isOnboarding: Boolean? = null) = capture(
        event = "view_login",
        properties = if (isOnboarding != null) mapOf("is_onboarding" to isOnboarding) else emptyMap(),
    )

    fun loginAttempt(autoLogin: Boolean) = capture(
        event = "login_attempt",
        properties = mapOf("auto_login" to autoLogin),
    )

    fun loginSuccess() = capture(
        event = "login_success",
        flushImmediately = true,
    )

    fun loginFail(errorType: LoginFailErrorType) = capture(
        event = "login_fail",
        properties = mapOf("error_type" to errorType.value),
        flushImmediately = true,
    )

    fun loginFailIfKnown(errorMessage: String) {
        loginErrorTypeFromMessage(errorMessage)?.let(::loginFail)
    }

    fun alarmPermission(
        isAllowed: Boolean,
        entryPoint: String = "onboarding",
    ) = capture(
        event = "alarm_permission",
        properties = mapOf(
            "is_allowed" to isAllowed,
            "entry_point" to entryPoint,
        ),
        flushImmediately = true,
    )

    fun refreshClick() = capture("refresh_click")

    fun pullToRefresh() = capture("pull_to_refresh")

    fun viewTaskDetail(
        detailType: String,
        entrySource: String,
    ) = capture(
        event = "view_task_detail",
        properties = mapOf(
            "detail_type" to detailType,
            "entry_source" to entrySource,
        ),
    )

    fun taskDetailTabClick(tabName: String) = capture(
        event = "task_detail_tab_click",
        properties = mapOf("tab_name" to tabName),
    )

    fun lmsLinkClick() = capture("lms_link_click")

    fun hideConfirm() = capture(
        event = "hide_confirm",
        flushImmediately = true,
    )

    fun submitCompleteClick() = capture(
        event = "submit_complete_click",
        flushImmediately = true,
    )

    fun viewCalendar() = capture("view_calendar")

    fun calendarDateClick() = capture("calendar_date_click")

    fun viewNotice() = capture("view_notice")

    fun noticeExpand(isUnread: Boolean) = capture(
        event = "notice_expand",
        properties = mapOf("is_unread" to isUnread),
    )

    fun viewHiddenTasks() = capture("view_hidden_tasks")

    fun restoreClick() = capture(
        event = "restore_click",
        flushImmediately = true,
    )

    fun cyberConnectClick(entryPoint: String) = capture(
        event = "cyber_connect_click",
        properties = mapOf("entry_point" to entryPoint),
    )

    fun cyberLoginSuccess() = capture(
        event = "cyber_login_success",
        flushImmediately = true,
    )

    fun cyberLoginFail() = capture(
        event = "cyber_login_fail",
        flushImmediately = true,
    )

    fun cyberFindIdClick() = capture("cyber_find_id_click")

    fun cyberDisconnectClick() = capture(
        event = "cyber_disconnect_click",
        flushImmediately = true,
    )

    fun notificationReceived(
        dDay: Int,
        notificationTaskCount: Int,
        representativeTodo: TodoInfo,
        notificationType: String? = null,
    ) = capture(
        event = "notification_received",
        properties = deadlineNotificationProperties(
            dDay = dDay,
            notificationTaskCount = notificationTaskCount,
            representativeTodo = representativeTodo,
            notificationType = notificationType,
        ),
    )

    fun notificationTap(
        dDay: Int,
        notificationTaskCount: Int,
        representativeTodo: TodoInfo,
        notificationType: String? = null,
    ) = capture(
        event = "notification_tap",
        properties = deadlineNotificationProperties(
            dDay = dDay,
            notificationTaskCount = notificationTaskCount,
            representativeTodo = representativeTodo,
            notificationType = notificationType,
        ),
    )

    fun viewHome(
        taskCount: Int,
        urgentCount: Int,
        entrySource: String,
    ) = capture(
        event = "view_home",
        properties = mapOf(
            "task_count" to taskCount,
            "urgent_count" to urgentCount,
            "entry_source" to entrySource,
        ),
    )

    fun viewMyPage() = capture("view_mypage")

    fun kakaoClick() = capture("kakao_click")

    fun settingSystemAlarm(isEnabled: Boolean) = capture(
        event = "setting_system_alarm",
        properties = mapOf("is_enabled" to isEnabled),
        flushImmediately = true,
    )

    fun logoutClick() = capture("logout_click")

    fun logoutConfirm() = capture(
        event = "logout_confirm",
        flushImmediately = true,
    )

    fun logoutCancel() = capture("logout_cancel")

    fun selectedTimeFromMinutes(minutes: Long): String? = when (minutes) {
        60L -> "1h"
        120L -> "2h"
        180L -> "3h"
        360L -> "6h"
        720L -> "12h"
        else -> null
    }

    private fun deadlineNotificationProperties(
        dDay: Int,
        notificationTaskCount: Int,
        representativeTodo: TodoInfo,
        notificationType: String? = null,
    ): Map<String, Any> = mapOf(
        "notification_type" to (notificationType ?: if (dDay == 0) "due_today" else "deadline_soon"),
        "notification_task_count" to notificationTaskCount,
        "d_day" to dDay,
        "task_type" to representativeTodo.type.kor,
        "subject_name" to representativeTodo.subject?.name.orEmpty(),
    )

    private fun loginErrorTypeFromMessage(errorMessage: String): LoginFailErrorType? {
        val normalized = errorMessage.lowercase()
        return if (
            normalized.contains("network") ||
            normalized.contains("timeout") ||
            normalized.contains("host") ||
            normalized.contains("connect")
        ) {
            LoginFailErrorType.NETWORK
        } else {
            LoginFailErrorType.WRONG_IDPW
        }
    }

    private fun String.toHashedDistinctId(): String? {
        val normalizedId = trim()
        if (normalizedId.isBlank()) return null

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedId.encodeToByteArray())
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
            }
        return "lms_sha256:$digest"
    }
}

enum class LoginFailErrorType(
    val value: String,
) {
    WRONG_IDPW("wrong_idpw"),
    NETWORK("network"),
}

fun TodoInfo.toDetailType(): String = when {
    type == TodoType.COMMONS || duration > 0 -> "video"
    attachments.isNotEmpty() -> "attachment"
    else -> "default"
}
