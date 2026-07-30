package com.yourssu.ssutime.v2.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.CALL_CHANNEL_ID
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.todo.localizedLabel
import java.util.concurrent.atomic.AtomicLong

fun showCallAlert(context: Context, todo: TodoInfo) {
    val notificationId = todo.todoId
    if (!CallAlertSession.tryStart(notificationId)) {
        Log.i(
            TAG,
            "이미 전화 알림이 표시 중이라 새 전화 알림을 건너뜁니다. " +
                "activeNotificationId=${CallAlertSession.activeNotificationId()}, skippedNotificationId=$notificationId",
        )
        return
    }

    Analytics.callAlertReceived(subjectName = todo.subject?.name.orEmpty())
    CallAlertRinger.start(context)

    val notificationShown = context.canPostNotifications()
    if (notificationShown) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            showCallStyleAlert(context, todo, notificationId)
        } else {
            showFallbackNotification(context, todo, notificationId)
        }
    }

    val activityStarted = context.startCallAlertActivity(todo, notificationId)
    if (!notificationShown && !activityStarted) {
        CallAlertRinger.stop(context)
        CallAlertSession.finish(notificationId)
    }
}

internal object CallAlertSession {
    private const val NO_ACTIVE_NOTIFICATION_ID = Long.MIN_VALUE
    private val activeNotificationId = AtomicLong(NO_ACTIVE_NOTIFICATION_ID)

    fun tryStart(notificationId: Int): Boolean =
        activeNotificationId.compareAndSet(NO_ACTIVE_NOTIFICATION_ID, notificationId.toLong())

    fun finish(notificationId: Int) {
        activeNotificationId.compareAndSet(notificationId.toLong(), NO_ACTIVE_NOTIFICATION_ID)
    }

    fun activeNotificationId(): Long? = activeNotificationId.get()
        .takeIf { it != NO_ACTIVE_NOTIFICATION_ID }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun showCallStyleAlert(context: Context, todo: TodoInfo, notificationId: Int) {
    context.logFullScreenIntentPermission()

    val incomingCaller = Person.Builder()
        .setName(todo.subject?.professor ?: todo.title)
        .setImportant(true)
        .build()
    val declineIntent = context.callActionIntent(notificationId, ACTION_DECLINE_CALL)
    val answerIntent = context.callAlertActivityPendingIntent(notificationId, todo, ACTION_ANSWER_CALL)
    val contentIntent = context.callAlertActivityPendingIntent(notificationId, todo, ACTION_OPEN_CALL_ALERT)
    val fullScreenIntent = context.callAlertActivityPendingIntent(notificationId, todo, ACTION_SHOW_CALL_ALERT)

    val notification = Notification.Builder(context, CALL_CHANNEL_ID)
        .setSmallIcon(R.drawable.outlined_checkbox)
        .setContentTitle(todo.title)
        .setContentText(context.toCallAlertText(todo))
        .setContentIntent(contentIntent)
        .setOngoing(true)
        .setFullScreenIntent(fullScreenIntent, true)
        .setAutoCancel(true)
        .setCategory(Notification.CATEGORY_CALL)
        .setPriority(Notification.PRIORITY_MAX)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setStyle(
            Notification.CallStyle.forIncomingCall(incomingCaller, declineIntent, answerIntent),
        )
        .addPerson(incomingCaller)
        .build()

    context.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
}

private fun showFallbackNotification(context: Context, todo: TodoInfo, notificationId: Int) {
    val fullScreenIntent = context.callAlertActivityPendingIntent(notificationId, todo, ACTION_SHOW_CALL_ALERT)
    val notification = Notification.Builder(context, CALL_CHANNEL_ID)
        .setSmallIcon(R.drawable.outlined_checkbox)
        .setContentTitle(context.getString(R.string.call_alert_notification_title))
        .setContentText(context.toCallAlertText(todo))
        .setStyle(Notification.BigTextStyle().bigText(context.toCallAlertText(todo)))
        .setContentIntent(context.callAlertActivityPendingIntent(notificationId, todo, ACTION_OPEN_CALL_ALERT))
        .setFullScreenIntent(fullScreenIntent, true)
        .setAutoCancel(true)
        .setCategory(Notification.CATEGORY_REMINDER)
        .setPriority(Notification.PRIORITY_MAX)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .build()

    context.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
}

class CallNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DECLINE_CALL) {
            Analytics.callAlertReject()
            val notificationId = intent.getIntExtra(EXTRA_CALL_NOTIFICATION_ID, 0)
            context.getSystemService(NotificationManager::class.java).cancel(notificationId)
            CallAlertRinger.stop(context)
            CallAlertSession.finish(notificationId)
        }
    }
}

private fun Context.canPostNotifications(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.logFullScreenIntentPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return
    }

    val notificationManager = getSystemService(NotificationManager::class.java)
    if (!notificationManager.canUseFullScreenIntent()) {
        Log.w(
            TAG,
            "전체화면 알림 권한이 꺼져 있습니다. Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT에서 허용해야 잠금화면/화면꺼짐 상태에서 풀스크린으로 뜹니다.",
        )
    }
}

private fun Context.callActionIntent(notificationId: Int, action: String): PendingIntent = PendingIntent.getBroadcast(
    this,
    notificationId,
    Intent(this, CallNotificationActionReceiver::class.java).apply {
        this.action = action
        putExtra(EXTRA_CALL_NOTIFICATION_ID, notificationId)
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun Context.callAlertActivityPendingIntent(
    notificationId: Int,
    todo: TodoInfo,
    action: String,
): PendingIntent = PendingIntent.getActivity(
    this,
    notificationId + action.hashCode(),
    callAlertActivityIntent(notificationId, todo).apply {
        this.action = action
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

internal fun Context.callAlertActivityIntent(notificationId: Int, todo: TodoInfo): Intent =
    Intent(this, CallAlertActivity::class.java).apply {
        putExtra(EXTRA_CALL_NOTIFICATION_ID, notificationId)
        putExtra(EXTRA_CALL_TODO_ID, todo.todoId)
        putExtra(EXTRA_CALL_TITLE, todo.title)
        putExtra(EXTRA_CALL_DUE_DATE, todo.due_date)
        putExtra(EXTRA_CALL_TODO_TYPE, todo.type.localizedLabel(this@callAlertActivityIntent))
        putExtra(EXTRA_CALL_SUBJECT_NAME, todo.subject?.name.orEmpty())
        putExtra(EXTRA_CALL_PROFESSOR, todo.subject?.professor.orEmpty())
    }

private fun Context.startCallAlertActivity(todo: TodoInfo, notificationId: Int): Boolean =
    runCatching {
        startActivity(
            callAlertActivityIntent(notificationId, todo).apply {
                action = ACTION_SHOW_CALL_ALERT
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }.onFailure { exception ->
        SentryExceptionReporter.capture(exception)
        Log.w(TAG, "통화 알림 Activity 실행이 시스템에 의해 거부되거나 실패했습니다.", exception)
    }.isSuccess

private fun Context.toCallAlertText(todo: TodoInfo): String {
    val subjectName = todo.subject?.name.orEmpty()
    val target = listOf(subjectName, todo.title)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    return getString(R.string.call_alert_text, target)
}

private const val TAG = "CallNotification"
private const val ACTION_DECLINE_CALL = "com.yourssu.ssutime.v2.notification.ACTION_DECLINE_CALL"
private const val ACTION_OPEN_CALL_ALERT = "com.yourssu.ssutime.v2.notification.ACTION_OPEN_CALL_ALERT"
internal const val ACTION_ANSWER_CALL = "com.yourssu.ssutime.v2.notification.ACTION_ANSWER_CALL"
internal const val ACTION_SHOW_CALL_ALERT = "com.yourssu.ssutime.v2.notification.ACTION_SHOW_CALL_ALERT"
internal const val EXTRA_CALL_NOTIFICATION_ID = "extra_call_notification_id"
internal const val EXTRA_CALL_TODO_ID = "extra_call_todo_id"
internal const val EXTRA_CALL_TITLE = "extra_call_title"
internal const val EXTRA_CALL_DUE_DATE = "extra_call_due_date"
internal const val EXTRA_CALL_TODO_TYPE = "extra_call_todo_type"
internal const val EXTRA_CALL_SUBJECT_NAME = "extra_call_subject_name"
internal const val EXTRA_CALL_PROFESSOR = "extra_call_professor"
