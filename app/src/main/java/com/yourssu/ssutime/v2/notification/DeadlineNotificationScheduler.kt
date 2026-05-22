package com.yourssu.ssutime.v2.notification

import android.Manifest
import android.app.PendingIntent
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DEADLINE_REMINDER_TAG = "deadline_reminder"

private val DEADLINE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
private val D_DAY_NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)
private val UPCOMING_NOTIFY_TIME: LocalTime = LocalTime.of(18, 0)
private val REFRESH_NOTIFICATION_WINDOW: Duration = Duration.ofMinutes(15)
private val DEADLINE_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM월 dd일 HH:mm", Locale.KOREA)

data class DeadlineReminderSendResult(
    val sentKeys: List<String> = emptyList(),
)

fun sendDeadlineNotificationsIfNeeded(
    context: Context,
    todoData: TodoData,
    now: Instant = Instant.now(),
): DeadlineReminderSendResult {
    cancelLegacyDeadlineWorkers(context)

    if (!context.canPostNotifications()) {
        return DeadlineReminderSendResult()
    }

    val pendingReminders = todoData.todos
        .flatMap { todo -> todo.toPendingReminderCandidates(now) }
        .filterNot { reminder -> reminder.key in todoData.sentDeadlineReminderKeys }

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
                        message = buildDdayDeadlineMessage(reminder.todo),
                        representativeTodo = reminder.todo,
                    )
                    sentKeys += reminder.key
                }
            } else {
                val sortedReminders = reminders.sortedBy { it.todo.due_date }
                val message = buildUpcomingDeadlineMessage(daysBefore, sortedReminders)
                val notificationKey = sortedReminders.joinToString(separator = "|") { it.key }
                context.showDeadlineNotification(
                    key = notificationKey,
                    message = message,
                    representativeTodo = sortedReminders.first().todo,
                )
                sentKeys += sortedReminders.map { it.key }
            }
        }

    return DeadlineReminderSendResult(sentKeys = sentKeys)
}

fun cancelDeadlineNotifications(context: Context) {
    cancelLegacyDeadlineWorkers(context)
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
    val dueInstant = due_date.toInstantOrNull() ?: return emptyList()
    val dueDate = dueInstant.atZone(DEADLINE_ZONE_ID).toLocalDate()

    return listOf(3, 2, 1, 0).mapNotNull { daysBefore ->
        val notifyTime = if (daysBefore == 0) D_DAY_NOTIFY_TIME else UPCOMING_NOTIFY_TIME
        val notifyAt = dueDate
            .minusDays(daysBefore.toLong())
            .atTime(notifyTime)
            .atZone(DEADLINE_ZONE_ID)
            .toInstant()

        if (!now.isInRefreshWindowAfter(notifyAt)) {
            return@mapNotNull null
        }

        DeadlineReminderCandidate(
            todo = this,
            daysBefore = daysBefore,
            key = deadlineReminderKey(daysBefore),
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

private fun Context.showDeadlineNotification(
    key: String,
    message: String,
    representativeTodo: TodoInfo,
) {
    val notificationId = key.hashCode()
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ssutime_launcher_foreground)
        .setContentTitle("마감 알림")
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setSubText(representativeTodo.subject?.name?.takeIf { it.isNotBlank() })
        .setContentIntent(mainActivityPendingIntent(notificationId))
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

private fun TodoInfo.deadlineReminderKey(daysBefore: Int): String =
    "deadline:$todoId:$due_date:$daysBefore"

private fun Instant.isInRefreshWindowAfter(target: Instant): Boolean =
    !isBefore(target) && !isAfter(target.plus(REFRESH_NOTIFICATION_WINDOW))

private fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun cancelLegacyDeadlineWorkers(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(DEADLINE_REMINDER_TAG)
}

private fun Context.mainActivityPendingIntent(requestCode: Int): PendingIntent = PendingIntent.getActivity(
    this,
    requestCode,
    Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun String.toInstantOrNull(): Instant? = runCatching {
    val parsedTime = DateTimeFormatter.ISO_DATE_TIME.parseBest(
        this,
        ZonedDateTime::from,
        OffsetDateTime::from,
        LocalDateTime::from,
    )

    when (parsedTime) {
        is ZonedDateTime -> parsedTime.toInstant()
        is OffsetDateTime -> parsedTime.toInstant()
        is LocalDateTime -> parsedTime.atZone(DEADLINE_ZONE_ID).toInstant()
        else -> error("Unsupported deadline time format: $this")
    }
}.getOrNull()

internal fun String.toDeadlineText(): String = toInstantOrNull()
    ?.atZone(DEADLINE_ZONE_ID)
    ?.format(DEADLINE_DATE_TIME_FORMATTER)
    .orEmpty()

private data class DeadlineReminderCandidate(
    val todo: TodoInfo,
    val daysBefore: Int,
    val key: String,
)
