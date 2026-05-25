package com.yourssu.ssutime.v2.fcm

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourssu.data.network.FcmRequest
import com.yourssu.ssutime.v2.CHANNEL_ID
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.v2.screen.main.RefreshSource
import com.yourssu.ssutime.v2.screen.main.TodoRefreshResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

private const val TAG = "LmsRefreshFCM"
private const val ACTION_CRAWL_LMS = "crawl_lms"
private const val REFRESH_TIMEOUT_MILLIS = 30_000L
private const val FCM_NOTIFICATION_FALLBACK_TITLE = "SSUTime"

class LmsRefreshMessagingService : FirebaseMessagingService(), KoinComponent {
    private val lmsRefreshRepository: LmsRefreshRepository by inject()
    private val loginRepository: LoginRepository by inject()
    private val apiRepository: ApiRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        runCatching {
            handleBackendMessage(message)
        }.onFailure { exception ->
            val detail = "FCM 처리 중 예외가 발생했습니다: ${exception.message ?: exception::class.java.simpleName}"
            Log.e(TAG, detail, exception)
        }
    }

    override fun onNewToken(token: String) {
        Log.i(TAG, "FCM token refreshed.")
        registerTokenIfLoggedIn(token)
    }

    private fun handleBackendMessage(message: RemoteMessage) {
        val action = message.data["action"].orEmpty()
        if (action == ACTION_CRAWL_LMS) {
            refreshTodos(message)
            return
        }

        val notification = message.notification
        if (notification != null) {
            showForegroundNotification(message)
            return
        }

        val actualAction = action.ifBlank { "-" }
        val detail = "지원하지 않는 FCM action입니다. expected=$ACTION_CRAWL_LMS, actual=$actualAction"
        Log.i(TAG, "$detail, data=${message.data}")
    }

    private fun refreshTodos(message: RemoteMessage) {
        val requestId = message.messageId
            ?: message.sentTime.takeIf { it > 0L }?.let { "fcm-$it" }
            ?: Instant.now().toString()

        val refreshResult = runBlocking {
            lmsRefreshRepository.refreshTodos(
                source = RefreshSource.FCM,
                requestId = requestId,
                timeoutMillis = REFRESH_TIMEOUT_MILLIS,
            )
        }

        when (refreshResult) {
            is TodoRefreshResult.Success -> Log.i(TAG, "백그라운드 새로고침이 완료되었습니다.")
            is TodoRefreshResult.Failure -> Log.e(TAG, refreshResult.message, refreshResult.throwable)
            is TodoRefreshResult.Skipped -> Log.i(TAG, refreshResult.reason)
        }
    }

    private fun registerTokenIfLoggedIn(token: String) {
        serviceScope.launch {
            val loginData = loginRepository.getLoginData()
            val bearerToken = accessToken.ifBlank { loginData.accessToken }
            if (bearerToken.isBlank()) {
                Log.i(TAG, "로그인 정보가 없어 FCM 토큰 등록을 건너뜁니다.")
                return@launch
            }

            accessToken = bearerToken
            runCatching {
                apiRepository.registerFCMToken(FcmRequest(token))
            }.onSuccess { status ->
                Log.i(TAG, "FCM 토큰이 등록되었습니다. status=$status")
            }.onFailure { exception ->
                Log.e(TAG, "FCM 토큰 등록에 실패했습니다.", exception)
            }
        }
    }

    private fun showForegroundNotification(message: RemoteMessage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "알림 권한이 없어 foreground FCM notification을 표시하지 않습니다.")
            return
        }

        val notification = message.notification ?: return
        val notificationId = message.messageId?.hashCode()
            ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val title = notification.title.orEmpty().ifBlank { FCM_NOTIFICATION_FALLBACK_TITLE }
        val body = notification.body.orEmpty()
        val appOpenIntent = PendingIntent.getActivity(
            this,
            notificationId,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ssutime_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(appOpenIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (body.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        NotificationManagerCompat.from(this).notify(notificationId, builder.build())
    }
}
