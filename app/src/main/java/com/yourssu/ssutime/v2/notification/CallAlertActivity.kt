package com.yourssu.ssutime.v2.notification

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.ui.theme.G400
import com.yourssu.ssutime.v2.ui.theme.R500
import com.yourssu.ssutime.v2.ui.theme.SSUTimeTheme
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CallAlertActivity : ComponentActivity() {
    private val notificationManager: NotificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }
    private var currentNotificationId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureCallWindow()
        val state = intent.toCallAlertState()
        currentNotificationId = state.notificationId
        if (intent.action == ACTION_ANSWER_CALL) {
            answerCall(state)
            return
        }
        CallAlertRinger.start(this)
        render(state)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val state = intent.toCallAlertState()
        currentNotificationId = state.notificationId
        if (intent.action == ACTION_ANSWER_CALL) {
            answerCall(state)
            return
        }
        render(state)
    }

    private fun render(state: CallAlertUiState) {
        setContent {
            SSUTimeTheme {
                IncomingCallScreen(
                    state = state,
                    onDecline = {
                        declineCall(state)
                    },
                    onAnswer = {
                        answerCall(state)
                    },
                )
            }
        }
    }

    private fun declineCall(state: CallAlertUiState) {
        Analytics.callAlertReject()
        notificationManager.cancel(state.notificationId)
        CallAlertRinger.stop(this)
        CallAlertSession.finish(state.notificationId)
        closeAppAfterDecline()
    }

    private fun answerCall(state: CallAlertUiState) {
        Analytics.callAlertAccept()
        notificationManager.cancel(state.notificationId)
        CallAlertRinger.stop(this)
        CallAlertSession.finish(state.notificationId)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_SKIP_INITIAL_LMS_REFRESH, true)
                putExtra(MainActivity.EXTRA_ENTRY_SOURCE, MainActivity.ENTRY_SOURCE_CALL_ALERT)
            }
        )
        finish()
    }

    private fun configureCallWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    private fun closeAppAfterDecline() {
        moveTaskToBack(true)
        finishAffinity()
    }

    override fun onDestroy() {
        CallAlertRinger.stop(this)
        currentNotificationId?.let(CallAlertSession::finish)
        super.onDestroy()
    }
}

private data class CallAlertUiState(
    val notificationId: Int,
    val todoId: Int,
    val title: String,
    val subjectName: String,
    val professor: String,
    val todoType: String,
    val dueDate: String,
)

private fun Intent.toCallAlertState(): CallAlertUiState =
    CallAlertUiState(
        notificationId = getIntExtra(EXTRA_CALL_NOTIFICATION_ID, 0),
        todoId = getIntExtra(EXTRA_CALL_TODO_ID, 0),
        title = getStringExtra(EXTRA_CALL_TITLE).orEmpty().ifBlank { "마감 직전 알림" },
        subjectName = getStringExtra(EXTRA_CALL_SUBJECT_NAME).orEmpty(),
        professor = getStringExtra(EXTRA_CALL_PROFESSOR).orEmpty(),
        todoType = getStringExtra(EXTRA_CALL_TODO_TYPE).orEmpty(),
        dueDate = getStringExtra(EXTRA_CALL_DUE_DATE).orEmpty(),
    )

@Composable
private fun IncomingCallScreen(
    state: CallAlertUiState,
    onDecline: () -> Unit,
    onAnswer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101113))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SSUTime",
            style = SSUType.H5SemiBold,
            color = Color(0xFFB9C0C7),
        )
        Spacer(Modifier.height(54.dp))
        CallerAvatar(
            text = state.professor.ifBlank {
                state.subjectName.ifBlank { "알림" }
            }.take(2),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = state.professor.ifBlank { "마감 알림" },
            style = SSUType.H1SemiBold,
            color = WHITE,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.subjectName.ifBlank { state.todoType.ifBlank { "LMS" } },
            style = SSUType.H4Medium,
            color = Color(0xFFCED4DA),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.title,
                style = SSUType.H2SemiBold,
                color = WHITE,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "마감 ${state.dueDate.toDisplayDueDate()}",
                style = SSUType.H5SemiBold,
                color = Color(0xFFFFD6D5),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallActionButton(
                label = "거절",
                color = R500,
                onClick = onDecline,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "거절",
                    tint = WHITE,
                )
            }
            Spacer(Modifier.weight(1f))
            CallActionButton(
                label = "응답",
                color = G400,
                onClick = onAnswer,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "응답",
                    tint = WHITE,
                )
            }
        }
    }
}

@Preview(
    name = "Incoming Call Screen",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun IncomingCallScreenPreview() {
    SSUTimeTheme {
        IncomingCallScreen(
            state = CallAlertUiState(
                notificationId = 0,
                todoId = 1,
                title = "데이터사이언스 분석 리포트 제출",
                subjectName = "데이터사이언스",
                professor = "김교수",
                todoType = "과제",
                dueDate = "2026-05-29T18:00:00Z",
            ),
            onDecline = {},
            onAnswer = {},
        )
    }
}

@Composable
private fun CallerAvatar(text: String) {
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(Color(0xFF2C3035)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.ifBlank { "알림" },
            style = SSUType.H1SemiBold,
            color = WHITE,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CallActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = label,
            style = SSUType.H5SemiBold,
            color = WHITE,
        )
    }
}

private fun String.toDisplayDueDate(): String =
    if (isBlank()) {
        "정보 없음"
    } else {
        runCatching {
            DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA)
                .withZone(ZoneId.of("Asia/Seoul"))
                .format(Instant.parse(this))
        }.getOrElse { this }
    }
