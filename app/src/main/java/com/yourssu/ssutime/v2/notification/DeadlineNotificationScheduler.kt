package com.yourssu.ssutime.v2.notification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.CHANNEL_ID
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.screen.main.notificationStore
import com.yourssu.ssutime.v2.screen.main.todoDataStore
import com.yourssu.ssutime.v2.todo.TODO_DEADLINE_ZONE_ID
import com.yourssu.ssutime.v2.todo.compareTodosByDeadlineThenName
import com.yourssu.ssutime.v2.todo.toTodoDeadlineInstantOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DEADLINE_REMINDER_TAG = "deadline_reminder"
private const val MORNING_REMINDER_REQUEST_CODE = 90_000
private const val EVENING_REMINDER_REQUEST_CODE = 180_000
private const val EXTRA_DEADLINE_REMINDER_TRIGGER_AT_MILLIS = "extra_deadline_reminder_trigger_at_millis"
private const val MAX_SENT_DEADLINE_REMINDER_KEYS = 500
private const val DEADLINE_REMINDER_KEY_VERSION = "deadline:v2"
private const val DEADLINE_REMINDER_KEY_SEPARATOR = "\u001F"

private val D_DAY_NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)
private val UPCOMING_NOTIFY_TIME: LocalTime = LocalTime.of(18, 0)
private val REFRESH_NOTIFICATION_WINDOW: Duration = Duration.ofMinutes(15)
private val DEADLINE_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM월 dd일 HH:mm", Locale.KOREA)

data class DeadlineReminderSendResult(
    val sentKeys: List<String> = emptyList(),
)

fun scheduleDeadlineNotifications(context: Context) {
    cancelLegacyDeadlineWorkers(context)
    scheduleDeadlineReminderAlarm(
        context = context.applicationContext,
        requestCode = MORNING_REMINDER_REQUEST_CODE,
        triggerTime = D_DAY_NOTIFY_TIME,
    )
    scheduleDeadlineReminderAlarm(
        context = context.applicationContext,
        requestCode = EVENING_REMINDER_REQUEST_CODE,
        triggerTime = UPCOMING_NOTIFY_TIME,
    )
}

fun sendDeadlineNotificationsIfNeeded(
    context: Context,
    todoData: TodoData,
    now: Instant = Instant.now(),
): DeadlineReminderSendResult {
    if (!context.canPostNotifications()) {
        return DeadlineReminderSendResult()
    }

    val pendingReminders = todoData.todos
        .flatMap { todo -> todo.toPendingReminderCandidates(now) }
        .filterNot { reminder ->
            reminder.key in todoData.sentDeadlineReminderKeys ||
                reminder.legacyKey in todoData.sentDeadlineReminderKeys
        }

    if (pendingReminders.isEmpty()) {
        return DeadlineReminderSendResult()
    }

    val sentKeys = mutableListOf<String>()
    pendingReminders
        .groupBy { it.daysBefore }
        .forEach { (daysBefore, reminders) ->
            if (daysBefore == 0) {
                reminders.forEach { reminder ->
                    context.showDeadlineNotification(
                        key = reminder.key,
                        title = buildDeadlineNotificationTitle(reminder.daysBefore),
                        message = buildDdayDeadlineMessage(reminder.todo),
                        representativeTodo = reminder.todo,
                        dDay = reminder.daysBefore,
                        notificationTaskCount = 1,
                    )
                    Analytics.notificationReceived(
                        dDay = reminder.daysBefore,
                        notificationTaskCount = 1,
                        representativeTodo = reminder.todo,
                    )
                    sentKeys += reminder.key
                }
            } else {
                val sortedReminders = reminders.sortedWith { left, right ->
                    compareTodosByDeadlineThenName(left.todo, right.todo)
                }
                val message = buildUpcomingDeadlineMessage(daysBefore, sortedReminders)
                val notificationKey = sortedReminders.joinToString(separator = "|") { it.key }
                context.showDeadlineNotification(
                    key = notificationKey,
                    title = buildDeadlineNotificationTitle(daysBefore),
                    message = message,
                    representativeTodo = sortedReminders.first().todo,
                    dDay = daysBefore,
                    notificationTaskCount = sortedReminders.size,
                )
                Analytics.notificationReceived(
                    dDay = daysBefore,
                    notificationTaskCount = sortedReminders.size,
                    representativeTodo = sortedReminders.first().todo,
                )
                sentKeys += sortedReminders.map { it.key }
            }
        }

    return DeadlineReminderSendResult(sentKeys = sentKeys)
}

fun cancelDeadlineNotifications(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    listOf(MORNING_REMINDER_REQUEST_CODE, EVENING_REMINDER_REQUEST_CODE).forEach { requestCode ->
        getExistingDeadlineReminderPendingIntent(context.applicationContext, requestCode)?.let { pendingIntent ->
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
    cancelLegacyDeadlineWorkers(context)
}

internal fun TodoData.withSentDeadlineReminderKeys(sentKeys: List<String>): TodoData {
    if (sentKeys.isEmpty()) {
        return this
    }

    return copy(
        sentDeadlineReminderKeys = (sentDeadlineReminderKeys + sentKeys)
            .distinct()
            .takeLast(MAX_SENT_DEADLINE_REMINDER_KEYS),
    )
}

internal fun buildDeadlineNotificationTitle(subjectName: String, title: String): String = listOf(
    subjectName.ifBlank { "공통" },
    title.ifBlank { "제목 없음" },
).joinToString(" · ")

internal fun buildDeadlineNotificationText(
    todoType: String,
    dueDate: String,
    daysBefore: Int,
): String {
    val itemName = todoType.ifBlank { "과제" }
    return if (daysBefore == 0) {
        "$itemName 오늘 마감이에요!"
    } else {
        "$itemName 마감 D-${daysBefore}이에요!"
    }
}

internal fun buildDeadlineNotificationBigText(
    title: String,
    subjectName: String,
    professor: String,
    todoType: String,
    dueDate: String,
    daysBefore: Int,
): String {
    val itemName = listOf(subjectName, todoType.ifBlank { "과제" })
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { title.ifBlank { "과제" } }

    return if (daysBefore == 0) {
        "$itemName 오늘 마감이에요!"
    } else {
        "$itemName 마감 D-${daysBefore}이에요!"
    }
}

private fun TodoInfo.toPendingReminderCandidates(now: Instant): List<DeadlineReminderCandidate> {
    val dueInstant = due_date.toTodoDeadlineInstantOrNull() ?: return emptyList()
    val dueDate = dueInstant.atZone(TODO_DEADLINE_ZONE_ID).toLocalDate()

    return listOf(3, 2, 1, 0).mapNotNull { daysBefore ->
        val notifyTime = if (daysBefore == 0) D_DAY_NOTIFY_TIME else UPCOMING_NOTIFY_TIME
        val notifyAt = dueDate
            .minusDays(daysBefore.toLong())
            .atTime(notifyTime)
            .atZone(TODO_DEADLINE_ZONE_ID)
            .toInstant()

        if (!now.isInRefreshWindowAfter(notifyAt)) {
            return@mapNotNull null
        }

        DeadlineReminderCandidate(
            todo = this,
            daysBefore = daysBefore,
            key = deadlineReminderKey(daysBefore),
            legacyKey = legacyDeadlineReminderKey(daysBefore),
        )
    }
}

private fun buildUpcomingDeadlineMessage(
    daysBefore: Int,
    reminders: List<DeadlineReminderCandidate>,
): String {
    val firstItemName = reminders.first().todo.toNotificationItemName()
    return if (reminders.size == 1) {
        "$firstItemName 마감 D-${daysBefore}이에요!"
    } else {
        "$firstItemName 외 ${reminders.size - 1}건, 곧 마감이에요!"
    }
}

private fun buildDdayDeadlineMessage(todo: TodoInfo): String =
    "${todo.toNotificationItemName()} 오늘 마감이에요!"

private fun buildDeadlineNotificationTitle(daysBefore: Int): String =
    if (daysBefore == 0) "오늘 마감, 아직 안 했죠?" else "마감이 다가오고 있어요!"

private fun Context.showDeadlineNotification(
    key: String,
    title: String,
    message: String,
    representativeTodo: TodoInfo,
    dDay: Int,
    notificationTaskCount: Int,
) {
    val notificationId = key.toStableNotificationId()
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ssutime_launcher_foreground)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setSubText(representativeTodo.subject?.name?.takeIf { it.isNotBlank() })
        .setContentIntent(
            mainActivityPendingIntent(
                requestCode = notificationId,
                dDay = dDay,
                notificationTaskCount = notificationTaskCount,
                representativeTodo = representativeTodo,
            ),
        )
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(this).notify(notificationId, notification)
}

private fun TodoInfo.toNotificationItemName(): String =
    listOf(
        subject?.name.orEmpty(),
        type.kor,
    ).filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { title.ifBlank { "과제" } }

internal fun TodoInfo.deadlineReminderKey(daysBefore: Int): String {
    val digest = listOf(
        todoId.toString(),
        due_date,
        daysBefore.toString(),
        type.name,
        subject?.id?.toString().orEmpty(),
        subject?.name.orEmpty(),
        subject?.professor.orEmpty(),
        title,
    )
        .joinToString(DEADLINE_REMINDER_KEY_SEPARATOR) { it.toReminderKeyPart() }
        .sha256Hex()

    return "$DEADLINE_REMINDER_KEY_VERSION:$digest"
}

private fun TodoInfo.legacyDeadlineReminderKey(daysBefore: Int): String =
    "deadline:$todoId:$due_date:$daysBefore"

internal fun String.toStableNotificationId(): Int {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return ((digest[0].toInt() and 0xFF) shl 24) or
        ((digest[1].toInt() and 0xFF) shl 16) or
        ((digest[2].toInt() and 0xFF) shl 8) or
        (digest[3].toInt() and 0xFF)
}

private fun String.toReminderKeyPart(): String =
    trim().replace(Regex("\\s+"), " ")

private fun String.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

private fun Instant.isInRefreshWindowAfter(target: Instant): Boolean =
    !isBefore(target) && !isAfter(target.plus(REFRESH_NOTIFICATION_WINDOW))

private fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun scheduleDeadlineReminderAlarm(
    context: Context,
    requestCode: Int,
    triggerTime: LocalTime,
) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val triggerAtMillis = nextDeadlineReminderTriggerTime(
        now = ZonedDateTime.now(TODO_DEADLINE_ZONE_ID),
        triggerTime = triggerTime,
    ).toInstant().toEpochMilli()
    val pendingIntent = deadlineReminderPendingIntent(
        context = context,
        requestCode = requestCode,
        triggerAtMillis = triggerAtMillis,
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    } else {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }
}

internal fun nextDeadlineReminderTriggerTime(
    now: ZonedDateTime,
    triggerTime: LocalTime,
): ZonedDateTime {
    val seoulNow = now.withZoneSameInstant(TODO_DEADLINE_ZONE_ID)
    val todayTrigger = seoulNow.toLocalDate()
        .atTime(triggerTime)
        .atZone(TODO_DEADLINE_ZONE_ID)
    return if (todayTrigger.isAfter(seoulNow)) todayTrigger else todayTrigger.plusDays(1)
}

private fun deadlineReminderPendingIntent(
    context: Context,
    requestCode: Int,
    triggerAtMillis: Long,
): PendingIntent = PendingIntent.getBroadcast(
    context,
    requestCode,
    Intent(context, DeadlineReminderReceiver::class.java).apply {
        putExtra(EXTRA_DEADLINE_REMINDER_TRIGGER_AT_MILLIS, triggerAtMillis)
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun getExistingDeadlineReminderPendingIntent(
    context: Context,
    requestCode: Int,
): PendingIntent? = PendingIntent.getBroadcast(
    context,
    requestCode,
    Intent(context, DeadlineReminderReceiver::class.java),
    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
)

private fun cancelLegacyDeadlineWorkers(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(DEADLINE_REMINDER_TAG)
}

private fun Context.mainActivityPendingIntent(
    requestCode: Int,
    dDay: Int,
    notificationTaskCount: Int,
    representativeTodo: TodoInfo,
): PendingIntent = PendingIntent.getActivity(
    this,
    requestCode,
    Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_ENTRY_SOURCE, MainActivity.ENTRY_SOURCE_NOTIFICATION)
        putExtra(MainActivity.EXTRA_NOTIFICATION_D_DAY, dDay)
        putExtra(MainActivity.EXTRA_NOTIFICATION_TASK_COUNT, notificationTaskCount)
        putExtra(MainActivity.EXTRA_NOTIFICATION_TASK_TYPE, representativeTodo.type.kor)
        putExtra(MainActivity.EXTRA_NOTIFICATION_SUBJECT_NAME, representativeTodo.subject?.name.orEmpty())
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

internal fun String.toDeadlineText(): String = toTodoDeadlineInstantOrNull()
    ?.atZone(TODO_DEADLINE_ZONE_ID)
    ?.format(DEADLINE_DATE_TIME_FORMATTER)
    .orEmpty()

private data class DeadlineReminderCandidate(
    val todo: TodoInfo,
    val daysBefore: Int,
    val key: String,
    val legacyKey: String,
)

class DeadlineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                sendScheduledDeadlineReminders(
                    context = context.applicationContext,
                    now = intent.getDeadlineReminderTriggerInstant(),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class DeadlineReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                rescheduleDeadlineNotificationsIfAllowed(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private suspend fun sendScheduledDeadlineReminders(
    context: Context,
    now: Instant,
) {
    val alertData = context.notificationStore.data.first()
    if (!alertData.allowSystemAlert) {
        cancelDeadlineNotifications(context)
        return
    }

    try {
        val todoData = context.todoDataStore.data.first()
        val result = sendDeadlineNotificationsIfNeeded(
            context = context,
            todoData = todoData,
            now = now,
        )
        if (result.sentKeys.isNotEmpty()) {
            context.todoDataStore.updateData { currentData ->
                currentData.withSentDeadlineReminderKeys(result.sentKeys)
            }
        }
    } finally {
        scheduleDeadlineNotifications(context)
    }
}

private suspend fun rescheduleDeadlineNotificationsIfAllowed(context: Context) {
    val alertData = context.notificationStore.data.first()
    if (alertData.allowSystemAlert) {
        scheduleDeadlineNotifications(context)
    } else {
        cancelDeadlineNotifications(context)
    }
}

private fun Intent.getDeadlineReminderTriggerInstant(): Instant {
    val triggerAtMillis = getLongExtra(EXTRA_DEADLINE_REMINDER_TRIGGER_AT_MILLIS, 0L)
    return if (triggerAtMillis > 0L) {
        Instant.ofEpochMilli(triggerAtMillis)
    } else {
        Instant.now()
    }
}
