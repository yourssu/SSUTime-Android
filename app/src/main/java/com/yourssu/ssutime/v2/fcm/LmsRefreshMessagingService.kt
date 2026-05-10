package com.yourssu.ssutime.v2.fcm

import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

class LmsRefreshMessagingService : com.google.firebase.messaging.FirebaseMessagingService(), KoinComponent {
    private val lmsRefreshRepository: com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] != _root_ide_package_.com.yourssu.ssutime.v2.LMS_REFRESH_MESSAGE_TYPE) {
            return
        }

        val requestId = message.data["requestId"]
            ?: message.messageId
            ?: Instant.now().toString()
        val refreshResult = runBlocking {
            lmsRefreshRepository.refreshTodos(
                source = _root_ide_package_.com.yourssu.ssutime.v2.screen.main.RefreshSource.FCM,
                requestId = requestId,
                timeoutMillis = REFRESH_TIMEOUT_MILLIS,
            )
        }

        when (refreshResult) {
            is com.yourssu.ssutime.v2.screen.main.TodoRefreshResult.Success -> Log.i(TAG, "백그라운드 새로고침이 완료되었습니다.")
            is com.yourssu.ssutime.v2.screen.main.TodoRefreshResult.Failure -> Log.e(TAG, refreshResult.message, refreshResult.throwable)
            is com.yourssu.ssutime.v2.screen.main.TodoRefreshResult.Skipped -> Log.i(TAG, refreshResult.reason)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed: $token")
    }

    private companion object {
        private const val TAG = "LmsRefreshFCM"
        private const val REFRESH_TIMEOUT_MILLIS = 30_000L
    }
}
