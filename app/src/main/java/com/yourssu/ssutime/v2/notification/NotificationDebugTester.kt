package com.yourssu.ssutime.v2.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.CHANNEL_ID
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.getRemainingDays
import kotlinx.coroutines.delay
import kotlin.math.max

private const val TAG = "NotificationDebugTester"
private const val DEBUG_NOTIFICATION_DELAY_MILLIS = 10_000L
private const val DEBUG_NORMAL_NOTIFICATION_ID = 20260517

suspend fun scheduleDebugCallAlert(
    context: Context,
    loadTodos: suspend () -> List<TodoInfo>,
) {
    delay(DEBUG_NOTIFICATION_DELAY_MILLIS)

    val todo = loadTodos().selectMostUrgentTodo()
    if (todo == null) {
        Log.i(TAG, "통화 알림 테스트에 사용할 Todo가 없습니다.")
        return
    }

    showCallAlert(context, todo)
}

suspend fun scheduleDebugNormalAlert(
    context: Context,
    loadTodos: suspend () -> List<TodoInfo>,
) {
    delay(DEBUG_NOTIFICATION_DELAY_MILLIS)

    val todo = loadTodos().selectMostUrgentTodo()
    if (todo == null) {
        Log.i(TAG, "일반 알림 테스트에 사용할 Todo가 없습니다.")
        return
    }

    showDebugNormalAlert(context, todo)
}

private fun showDebugNormalAlert(context: Context, todo: TodoInfo) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val daysBefore = max(0, getRemainingDays(todo.due_date).toInt())
    val subjectName = todo.subject?.name.orEmpty()
    val professor = todo.subject?.professor.orEmpty()
    val title = buildDeadlineNotificationTitle(subjectName, todo.title)
    val text = buildDeadlineNotificationText(todo.type.kor, todo.due_date, daysBefore)
    val bigText = buildDeadlineNotificationBigText(
        todo.title,
        subjectName,
        professor,
        todo.type.kor,
        todo.due_date,
        daysBefore,
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ssutime_launcher_foreground)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        .setSubText(subjectName.takeIf { it.isNotBlank() })
        .setContentIntent(context.mainActivityPendingIntent(DEBUG_NORMAL_NOTIFICATION_ID))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context).notify(DEBUG_NORMAL_NOTIFICATION_ID, notification)
}

private fun List<TodoInfo>.selectMostUrgentTodo(): TodoInfo? = minWithOrNull(
    compareBy<TodoInfo> { getRemainingDays(it.due_date) }
        .thenBy { it.due_date },
)

private fun Context.mainActivityPendingIntent(requestCode: Int): PendingIntent = PendingIntent.getActivity(
    this,
    requestCode,
    Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
