package com.yourssu.ssutime.fcm

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourssu.ssutime.CHANNEL_ID
import com.yourssu.ssutime.LMS_REFRESH_MESSAGE_TYPE
import com.yourssu.ssutime.MainActivity
import com.yourssu.ssutime.R
import com.yourssu.ssutime.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.screen.main.RefreshSource
import com.yourssu.ssutime.screen.main.TodoRefreshResult
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class LmsRefreshMessagingService : FirebaseMessagingService(), KoinComponent {
    private val lmsRefreshRepository: LmsRefreshRepository by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] != LMS_REFRESH_MESSAGE_TYPE) {
            return
        }

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
            is TodoRefreshResult.Success -> showRefreshSuccessNotification(refreshResult)
            is TodoRefreshResult.Failure -> showRefreshFailureNotification(refreshResult)
            is TodoRefreshResult.Skipped -> Log.i(TAG, refreshResult.reason)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed: $token")
    }

    private fun showRefreshSuccessNotification(result: TodoRefreshResult.Success) {
        val completedTime = result.summary.completedAt.toDisplayTime()
        val title = "백그라운드 새로고침 완료"
        val content = "${completedTime}에 할 일 ${result.summary.todoCount}개를 불러왔어요."
        val detail = "학기 ${result.summary.semesterCount}개, 과목 ${result.summary.subjectCount}개, 제출 ${result.summary.submittedCount}개 갱신"

        showNotification(title, content, detail)
    }

    private fun showRefreshFailureNotification(result: TodoRefreshResult.Failure) {
        val now = Instant.now().toDisplayTime()
        showNotification(
            title = "백그라운드 새로고침 실패",
            content = "${now}에 LMS 정보를 불러오지 못했어요.",
            detail = result.message,
        )
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, content: String, detail: String) {
        if (!canPostNotifications()) {
            Log.i(TAG, "알림 권한이 없어 새로고침 결과 알림을 표시하지 않습니다.")
            return
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.done)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(createContentIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(REFRESH_NOTIFICATION_ID, notification)
        } catch (security: SecurityException) {
            Log.e(TAG, "새로고침 결과 알림을 표시하지 못했습니다.", security)
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun Instant.toDisplayTime(): String {
        val pattern = if (DateFormat.is24HourFormat(this@LmsRefreshMessagingService)) {
            "HH:mm"
        } else {
            "a h:mm"
        }
        return atZone(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern(pattern, Locale.KOREA))
    }

    private companion object {
        private const val TAG = "LmsRefreshFCM"
        private const val REFRESH_TIMEOUT_MILLIS = 30_000L
        private const val REFRESH_NOTIFICATION_ID = 1001
    }
}
