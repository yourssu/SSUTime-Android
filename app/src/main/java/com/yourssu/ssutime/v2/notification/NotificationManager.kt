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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.CHANNEL_ID
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R

fun showCallAlert(context: Context, todo: TodoInfo) {
    if (!context.canPostNotifications()) {
        return
    }

    val notificationId = todo.todoId

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        showCallStyleAlert(context, todo, notificationId)
    } else {
        showFallbackNotification(context, todo, notificationId)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun showCallStyleAlert(context: Context, todo: TodoInfo, notificationId: Int) {
    val incomingCaller = Person.Builder()
        .setName(todo.subject?.professor ?: todo.title)
        .setImportant(true)
        .build()
    val declineIntent = context.callActionIntent(notificationId, ACTION_DECLINE_CALL)
    val answerIntent = context.mainActivityIntent(notificationId, ACTION_ANSWER_CALL, shouldCancelNotification = true)
    val contentIntent = context.mainActivityIntent(notificationId, ACTION_OPEN_CALL_ALERT, shouldCancelNotification = false)
    val fullScreenIntent = context.mainActivityIntent(notificationId, ACTION_SHOW_CALL_ALERT, shouldCancelNotification = false)

    val notification = Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.avatar_container)
        .setContentTitle(todo.title)
        .setContentText(todo.toCallAlertText())
        .setContentIntent(contentIntent)
        .setOngoing(true)
        .setFullScreenIntent(fullScreenIntent, true)
        .setAutoCancel(true)
        .setCategory(Notification.CATEGORY_CALL)
        .setStyle(
            Notification.CallStyle.forIncomingCall(incomingCaller, declineIntent, answerIntent),
        )
        .addPerson(incomingCaller)
        .build()

    context.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
}

private fun showFallbackNotification(context: Context, todo: TodoInfo, notificationId: Int) {
    val notification = Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ssutime_launcher_foreground)
        .setContentTitle("마감 직전 알림")
        .setContentText(todo.toCallAlertText())
        .setStyle(Notification.BigTextStyle().bigText(todo.toCallAlertText()))
        .setContentIntent(context.mainActivityIntent(notificationId, ACTION_OPEN_CALL_ALERT, shouldCancelNotification = false))
        .setAutoCancel(true)
        .setCategory(Notification.CATEGORY_REMINDER)
        .build()

    context.getSystemService(NotificationManager::class.java).notify(notificationId, notification)
}

class CallNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DECLINE_CALL) {
            val notificationId = intent.getIntExtra(EXTRA_CALL_NOTIFICATION_ID, 0)
            context.getSystemService(NotificationManager::class.java).cancel(notificationId)
        }
    }
}

private fun Context.canPostNotifications(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.callActionIntent(notificationId: Int, action: String): PendingIntent = PendingIntent.getBroadcast(
    this,
    notificationId,
    Intent(this, CallNotificationActionReceiver::class.java).apply {
        this.action = action
        putExtra(EXTRA_CALL_NOTIFICATION_ID, notificationId)
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun Context.mainActivityIntent(
    notificationId: Int,
    action: String,
    shouldCancelNotification: Boolean,
): PendingIntent = PendingIntent.getActivity(
    this,
    notificationId + action.hashCode(),
    Intent(this, MainActivity::class.java).apply {
        this.action = action
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (shouldCancelNotification) {
            putExtra(EXTRA_CALL_NOTIFICATION_ID, notificationId)
        }
    },
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun TodoInfo.toCallAlertText(): String {
    val subjectName = subject?.name.orEmpty()
    val target = listOf(subjectName, title)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    return "$target 마감 직전이에요."
}

private const val ACTION_DECLINE_CALL = "com.yourssu.ssutime.v2.notification.ACTION_DECLINE_CALL"
private const val ACTION_OPEN_CALL_ALERT = "com.yourssu.ssutime.v2.notification.ACTION_OPEN_CALL_ALERT"
internal const val ACTION_ANSWER_CALL = "com.yourssu.ssutime.v2.notification.ACTION_ANSWER_CALL"
internal const val ACTION_SHOW_CALL_ALERT = "com.yourssu.ssutime.v2.notification.ACTION_SHOW_CALL_ALERT"
internal const val EXTRA_CALL_NOTIFICATION_ID = "extra_call_notification_id"

@Composable
fun IncomingCallScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("전화가 왔습니다", color = Color.White, fontSize = 28.sp)
        Spacer(Modifier.height(40.dp))

        Row {
            Button(onClick = { /* 거절 처리 */ }) {
                Text("거절")
            }

            Spacer(Modifier.width(24.dp))

            Button(onClick = { /* 수락 처리 */ }) {
                Text("수락")
            }
        }
    }
}
