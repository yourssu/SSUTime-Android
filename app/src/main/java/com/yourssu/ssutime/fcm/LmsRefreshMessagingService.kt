package com.yourssu.ssutime.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourssu.ssutime.LMS_REFRESH_MESSAGE_TYPE
import com.yourssu.ssutime.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.screen.main.RefreshSource
import com.yourssu.ssutime.screen.main.TodoRefreshResult
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

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
            is TodoRefreshResult.Success -> Log.i(TAG, "백그라운드 새로고침이 완료되었습니다.")
            is TodoRefreshResult.Failure -> Log.e(TAG, refreshResult.message, refreshResult.throwable)
            is TodoRefreshResult.Skipped -> Log.i(TAG, refreshResult.reason)
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
