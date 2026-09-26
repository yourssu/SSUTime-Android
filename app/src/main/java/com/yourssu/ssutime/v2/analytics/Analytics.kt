package com.yourssu.ssutime.v2.analytics

import android.content.Context
import android.util.Log
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

object Analytics {
    private const val TAG = "Analytics"
    private const val INSTALL_ATTRIBUTION_WAIT_MILLIS = 2_000L
    private val installAttributionHandled = CompletableDeferred<Unit>()
    private val identifiedDistinctId = AtomicReference<String?>(null)

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        applicationScope.launch(block = block)

    fun setup(context: Context) {
        val apiKey = BuildConfig.POSTHOG_API_KEY
        if (apiKey.isBlank()) {
            Log.i(TAG, "PostHog API key is empty. Analytics disabled.")
            return
        }

        val config = PostHogAndroidConfig(
            apiKey = apiKey,
            captureApplicationLifecycleEvents = false,
            captureDeepLinks = false,
            captureScreenViews = false,
        ).apply {
            debug = BuildConfig.DEBUG_MODE
            flushIntervalSeconds = 5
            flushAt = if (BuildConfig.DEBUG_MODE) 1 else 10
        }
        PostHogAndroid.setup(context, config)
    }

    fun flush() {
        applicationScope.launch {
            runCatching {
                PostHog.flush()
            }.onFailure { exception ->
                SentryExceptionReporter.capture(exception)
                Log.w(TAG, "Failed to flush events.", exception)
            }
        }
    }

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

    fun loginFail(errorType: LoginFailErrorType? = null) = capture(
        event = "login_fail",
        flushImmediately = true,
    )

    fun loginFailIfKnown(errorMessage: String) {
        loginErrorTypeFromMessage(errorMessage)?.let(::loginFail)
    }

    fun alarmPermission(
        isAllowed: Boolean,
    ) = capture(
        event = "alarm_permission",
        properties = mapOf(
            "is_allowed" to isAllowed,
        ),
        flushImmediately = true,
    )

    fun refreshClick() = capture("refresh_click")

    fun pullToRefresh() = capture("pull_to_refresh")

    fun submittedAttachmentDownload() = capture("submitted_attachment_download")

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

    fun taskAttachmentClick() = capture("task_attachment_click")

    fun hideConfirm() = capture(
        event = "hide_confirm",
        flushImmediately = true,
    )

    @Deprecated("Replaced by viewTaskDetail in 26-2")
    fun taskDetailExpand(
        todo: TodoInfo,
        dDay: Int,
        hasAiSummary: Boolean,
    ) {
        val properties = mutableMapOf<String, Any>(
            "task_type" to todo.type.kor,
            "d_day" to dDay,
            "subject_name" to todo.subject?.name.orEmpty(),
            "has_ai_summary" to hasAiSummary,
        )

        capture(
            event = "task_detail_expand",
            properties = properties,
        )
    }

    @Deprecated("Replaced by viewTaskDetail in 26-2")
    fun taskDetailCollapse() = capture("task_detail_collapse")

    fun submitCompleteClick() = capture(
        event = "submit_complete_click",
        flushImmediately = true,
    )

    fun widgetBannerClick() = capture("widget_banner_click")

    fun widgetBannerDismiss() = capture("widget_banner_dismiss")

    fun widgetBannerConfirm() = capture("widget_banner_confirm")

    fun viewCalendar() = capture("view_calendar")

    fun calendarMonthChange(direction: String) = capture(
        event = "calendar_month_change",
        properties = mapOf("direction" to direction),
    )

    fun calendarDateClick() = capture("calendar_date_click")

    fun viewNotice() = capture("view_notice")

    fun noticeExpand(isUnread: Boolean) = capture(
        event = "notice_expand",
        properties = mapOf("is_unread" to isUnread),
    )

    fun callAlarmRepopupView() = capture("call_alarm_repopup_view")

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

    fun cyberBannerDismiss() = capture("cyber_banner_dismiss")

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

    fun callAlertReceived(subjectName: String) = capture(
        event = "call_alert_received",
        properties = mapOf("subject_name" to subjectName),
    )

    fun callAlertAccept() = capture("call_alert_accept")

    fun callAlertReject() = capture("call_alert_reject")

    fun widgetTap(widgetSize: String) = capture(
        event = "widget_tap",
        properties = mapOf("widget_size" to widgetSize),
    )

    fun widgetDisplay(widgetSize: String) = capture(
        event = "widget_display",
        properties = mapOf("widget_size" to widgetSize),
    )

    fun widgetRefreshTap(
        refreshResult: Boolean,
        widgetSize: String,
    ) = capture(
        event = "widget_refresh_tap",
        properties = mapOf(
            "refresh_result" to refreshResult,
            "widget_size" to widgetSize,
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

    fun settingCallAlarm(
        isEnabled: Boolean,
        selectedTime: String,
    ) = capture(
        event = "setting_call_alarm",
        properties = mapOf(
            "is_enabled" to isEnabled,
            "selected_time" to selectedTime,
        ),
        flushImmediately = true,
    )

    fun callAlarmSetting(
        selectedTime: String,
        entryPoint: String = "repopup",
    ) = capture(
        event = "call_alarm_setting",
        properties = mapOf(
            "entry_point" to entryPoint,
            "selected_time" to selectedTime,
        ),
        flushImmediately = true,
    )

    fun settingLabMode(isEnabled: Boolean) = capture(
        event = "setting_lab_mode",
        properties = mapOf("is_enabled" to isEnabled),
        flushImmediately = true,
    )

    fun withdrawClick() = capture(
        event = "withdraw_click",
        flushImmediately = true,
    )

    fun logoutClick() = capture("logout_click")

    fun logoutConfirm() = capture(
        event = "logout_confirm",
        flushImmediately = true,
    )

    fun logoutCancel() = capture("logout_cancel")

    fun postHogDistinctId(loginId: String): String? = loginId.toHashedDistinctId()

    fun currentPostHogDistinctId(): String? = identifiedDistinctId.get()

    fun appStoreInstalled(utmProperties: Map<String, String>) {
        val properties = utmProperties.filterKeys { key ->
            key.startsWith("utm_")
        }.toMutableMap<String, Any>()
        val utmSource = utmProperties["utm_source"] ?: utmProperties["UTM source"]
        if (!utmSource.isNullOrBlank()) {
            properties["UTM source"] = utmSource
        }
        properties["platform"] = "android"

        capture(
            event = "app_store_installed",
            properties = properties,
            flushImmediately = true,
        )
    }

    suspend fun identifyUser(loginId: String) {
        withContext(applicationScope.coroutineContext + NonCancellable) {
            val distinctId = postHogDistinctId(loginId) ?: return@withContext
            waitForInstallAttribution()
            if (identifiedDistinctId.get() == distinctId) return@withContext

            runCatching {
                PostHog.identify(distinctId = distinctId)
                identifiedDistinctId.set(distinctId)
                flush()
            }.onFailure { exception ->
                SentryExceptionReporter.capture(exception)
                Log.w(TAG, "Failed to identify user.", exception)
            }
        }
    }

    fun resetUser() {
        applicationScope.launch {
            identifiedDistinctId.set(null)
            runCatching {
                PostHog.reset()
                flush()
            }.onFailure { exception ->
                SentryExceptionReporter.capture(exception)
                Log.w(TAG, "Failed to reset user.", exception)
            }
        }
    }

    internal fun markInstallAttributionHandled() {
        if (!installAttributionHandled.isCompleted) {
            installAttributionHandled.complete(Unit)
        }
    }

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
            // TODO: LMS 로그인 실패 응답에서 아이디/비밀번호 오류가 구분되면 wrong_id/wrong_pw를 연결하세요.
            LoginFailErrorType.WRONG_IDPW
        }
    }

    private fun capture(
        event: String,
        properties: Map<String, Any> = emptyMap(),
        flushImmediately: Boolean = false,
    ) {
        runCatching {
            PostHog.capture(
                event = event,
                properties = properties,
            )
            if (flushImmediately) {
                flush()
            }
        }.onFailure { exception ->
            SentryExceptionReporter.capture(exception)
            Log.w(TAG, "Failed to capture event: $event", exception)
        }
    }

    private suspend fun waitForInstallAttribution() {
        withTimeoutOrNull(INSTALL_ATTRIBUTION_WAIT_MILLIS) {
            installAttributionHandled.await()
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
