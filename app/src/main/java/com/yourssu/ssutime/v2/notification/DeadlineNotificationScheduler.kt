package com.yourssu.ssutime.v2.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.CHANNEL_ID
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DEADLINE_REMINDER_TAG = "deadline_reminder"
private const val KEY_TODO_ID = "todo_id"
private const val KEY_TODO_TITLE = "todo_title"
private const val KEY_SUBJECT_NAME = "subject_name"
private const val KEY_PROFESSOR = "professor"
private const val KEY_TODO_TYPE = "todo_type"
private const val KEY_DUE_DATE = "due_date"
private const val KEY_DAYS_BEFORE = "days_before"

private val DEADLINE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
private val DEADLINE_NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)
private val DEADLINE_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm", Locale.KOREA)

fun scheduleDeadlineNotifications(context: Context, todos: List<TodoInfo>) {
    val workManager = WorkManager.getInstance(context)
    val now = Instant.now()
    val requests = todos.flatMap { todo ->
        val dueInstant = todo.due_date.toInstantOrNull() ?: return@flatMap emptyList()
        val dueDate = dueInstant.atZone(DEADLINE_ZONE_ID).toLocalDate()

        listOf(3, 2, 1, 0).mapNotNull { daysBefore ->
            val notifyAt = dueDate
                .minusDays(daysBefore.toLong())
                .atTime(DEADLINE_NOTIFY_TIME)
                .atZone(DEADLINE_ZONE_ID)
                .toInstant()

            if (!notifyAt.isAfter(now)) return@mapNotNull null

            OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
                .setInitialDelay(Duration.between(now, notifyAt))
                .setInputData(
                    Data.Builder()
                        .putInt(KEY_TODO_ID, todo.todoId)
                        .putString(KEY_TODO_TITLE, todo.title)
                        .putString(KEY_SUBJECT_NAME, todo.subject?.name.orEmpty())
                        .putString(KEY_PROFESSOR, todo.subject?.professor.orEmpty())
                        .putString(KEY_TODO_TYPE, todo.type.kor)
                        .putString(KEY_DUE_DATE, todo.due_date)
                        .putInt(KEY_DAYS_BEFORE, daysBefore)
                        .build(),
                )
                .addTag(DEADLINE_REMINDER_TAG)
                .build()
        }
    }

    workManager.cancelAllWorkByTag(DEADLINE_REMINDER_TAG).result.addListener(
        {
            if (requests.isNotEmpty()) {
                workManager.enqueue(requests)
            }
        },
        ContextCompat.getMainExecutor(context),
    )
}

fun cancelDeadlineNotifications(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(DEADLINE_REMINDER_TAG)
}

class DeadlineNotificationWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : Worker(context, workerParameters) {

    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val todoId = inputData.getInt(KEY_TODO_ID, 0)
        val title = inputData.getString(KEY_TODO_TITLE).orEmpty()
        val subjectName = inputData.getString(KEY_SUBJECT_NAME).orEmpty()
        val professor = inputData.getString(KEY_PROFESSOR).orEmpty()
        val todoType = inputData.getString(KEY_TODO_TYPE).orEmpty()
        val dueDate = inputData.getString(KEY_DUE_DATE).orEmpty()
        val daysBefore = inputData.getInt(KEY_DAYS_BEFORE, 0)
        val notificationId = "deadline_${todoId}_${dueDate}_${title}_$daysBefore".hashCode()
        val notificationTitle = buildDeadlineNotificationTitle(subjectName, title)
        val notificationText = buildDeadlineNotificationText(todoType, dueDate, daysBefore)
        val notificationBigText = buildDeadlineNotificationBigText(
            title,
            subjectName,
            professor,
            todoType,
            dueDate,
            daysBefore,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ssutime_launcher_foreground)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationBigText),
            )
            .setSubText(subjectName.takeIf { it.isNotBlank() })
            .setContentIntent(context.mainActivityPendingIntent(notificationId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return Result.success()
    }

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
    val dueText = dueDate.toDeadlineText().ifBlank { "마감 시간 정보 없음" }
    val dDayText = if (daysBefore == 0) "오늘 마감" else "D-$daysBefore"

    return "$dueText 마감 · $dDayText · ${todoType.ifBlank { "과제" }}"
}

internal fun buildDeadlineNotificationBigText(
    title: String,
    subjectName: String,
    professor: String,
    todoType: String,
    dueDate: String,
    daysBefore: Int,
): String {
    val dueText = dueDate.toDeadlineText().ifBlank { "마감 시간 정보 없음" }
    val dDayText = if (daysBefore == 0) "오늘 마감" else "D-$daysBefore"

    return listOf(
        "과목: ${subjectName.ifBlank { "공통" }}",
        "교수: ${professor.ifBlank { "정보 없음" }}",
        "과제: ${title.ifBlank { "제목 없음" }}",
        "유형: ${todoType.ifBlank { "과제" }}",
        "마감: $dueText",
        "남은 기간: $dDayText",
    ).joinToString("\n")
}

private fun Context.mainActivityPendingIntent(requestCode: Int) = android.app.PendingIntent.getActivity(
    this,
    requestCode,
    Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    },
    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
)

private fun String.toInstantOrNull(): Instant? = runCatching {
    Instant.parse(this)
}.recoverCatching {
    LocalDateTime.parse(removeSuffix("Z"))
        .atZone(DEADLINE_ZONE_ID)
        .toInstant()
}.getOrNull()

internal fun String.toDeadlineText(): String = toInstantOrNull()
    ?.atZone(DEADLINE_ZONE_ID)
    ?.format(DEADLINE_DATE_TIME_FORMATTER)
    .orEmpty()
