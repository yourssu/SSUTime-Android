package com.yourssu.ssutime.v2.analytics

import android.content.Context
import android.util.Log
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.BuildConfig

object Analytics {
    private const val TAG = "Analytics"

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
            debug = BuildConfig.DEBUG
        }
        PostHogAndroid.setup(context, config)
    }

    fun viewLogin() = capture("view_login")

    fun loginAttempt(autoLogin: Boolean) = capture(
        event = "login_attempt",
        properties = mapOf("auto_login" to autoLogin),
    )

    fun loginSuccess() = capture("login_success")

    fun loginFail(errorType: LoginFailErrorType) = capture(
        event = "login_fail",
        properties = mapOf("error_type" to errorType.value),
    )

    fun loginFailIfKnown(errorMessage: String) {
        loginErrorTypeFromMessage(errorMessage)?.let(::loginFail)
    }

    fun alarmPermission(isAllowed: Boolean) = capture(
        event = "alarm_permission",
        properties = mapOf("is_allowed" to isAllowed),
    )

    fun refreshClick() = capture("refresh_click")

    fun pullToRefresh() = capture("pull_to_refresh")

    fun taskDetailExpand(
        todo: TodoInfo,
        dDay: Int,
        hasAiSummary: Boolean = false,
    ) = capture(
        event = "task_detail_expand",
        properties = mapOf(
            "task_type" to todo.type.kor,
            "d_day" to dDay,
            "subject_name" to todo.subject?.name.orEmpty(),
            "has_ai_summary" to hasAiSummary,
        ),
    )

    fun taskDetailCollapse() = capture("task_detail_collapse")

    fun submitCompleteClick() = capture("submit_complete_click")

    fun widgetBannerClick() = capture("widget_banner_click")

    fun widgetBannerDismiss() = capture("widget_banner_dismiss")

    fun notificationReceived(
        dDay: Int,
        notificationTaskCount: Int,
        representativeTodo: TodoInfo,
    ) = capture(
        event = "notification_received",
        properties = deadlineNotificationProperties(
            dDay = dDay,
            notificationTaskCount = notificationTaskCount,
            representativeTodo = representativeTodo,
        ),
    )

    fun notificationTap(
        dDay: Int,
        notificationTaskCount: Int,
        representativeTodo: TodoInfo,
    ) = capture(
        event = "notification_tap",
        properties = deadlineNotificationProperties(
            dDay = dDay,
            notificationTaskCount = notificationTaskCount,
            representativeTodo = representativeTodo,
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

    fun settingSystemAlarm(isEnabled: Boolean) = capture(
        event = "setting_system_alarm",
        properties = mapOf("is_enabled" to isEnabled),
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
    )

    fun callAlarmSetting(selectedTime: String) = capture(
        event = "call_alarm_setting",
        properties = mapOf("selected_time" to selectedTime),
    )

    fun logoutClick() = capture("logout_click")

    fun logoutConfirm() = capture("logout_confirm")

    fun logoutCancel() = capture("logout_cancel")

    fun selectedTimeFromMinutes(minutes: Long): String? = when (minutes) {
        60L -> "1h"
        120L -> "2h"
        360L -> "6h"
        else -> null
    }

    private fun deadlineNotificationProperties(
        dDay: Int,
        notificationTaskCount: Int,
        representativeTodo: TodoInfo,
    ): Map<String, Any> = mapOf(
        "notification_type" to if (dDay == 0) "due_today" else "deadline_soon",
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
    ) {
        runCatching {
            PostHog.capture(
                event = event,
                properties = properties,
            )
        }.onFailure { exception ->
            Log.w(TAG, "Failed to capture event: $event", exception)
        }
    }
}

enum class LoginFailErrorType(
    val value: String,
) {
    WRONG_IDPW("wrong_idpw"),
    NETWORK("network"),
}
