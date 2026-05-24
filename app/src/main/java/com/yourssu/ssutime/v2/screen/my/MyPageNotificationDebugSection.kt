package com.yourssu.ssutime.v2.screen.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.fcm.FCM_DEBUG_HISTORY_ENABLED
import com.yourssu.ssutime.v2.fcm.FcmDebugRecord
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N300
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationDebugSection(
    onScheduleCallAlert: () -> Unit,
    onScheduleNormalAlert: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    fcmToken: String,
    fcmTokenRegistrationStatus: String,
    onRefreshFcmToken: () -> Unit,
    onRegisterFcmToken: () -> Unit,
    fcmRecords: List<FcmDebugRecord>,
    onClearFcmHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "임시 알림 테스트",
            style = SSUType.H5SemiBold,
        )
        Text(
            text = "버튼을 누르면 10초 후 저장된 가장 임박한 할 일로 테스트해요.",
            style = SSUType.Caption1Medium,
            color = N300,
        )
        OptionButton(
            text = "10초 후 통화 알림 테스트",
            onClick = onScheduleCallAlert,
        )
        OptionButton(
            text = "10초 후 일반 알림 테스트",
            onClick = onScheduleNormalAlert,
        )
        OptionButton(
            text = "전체화면 알림 권한 설정",
            onClick = onOpenFullScreenIntentSettings,
        )

        if (FCM_DEBUG_HISTORY_ENABLED) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = "내 FCM 토큰",
                style = SSUType.H5SemiBold,
            )
            Text(
                text = "현재 설치된 앱 인스턴스에 발급된 토큰입니다.",
                style = SSUType.Caption1Medium,
                color = N300,
            )
            FcmTokenCard(fcmToken)
            OptionButton(
                text = "FCM 토큰 다시 불러오기",
                onClick = onRefreshFcmToken,
            )
            OptionButton(
                text = "현재 FCM 토큰 서버 재등록",
                onClick = onRegisterFcmToken,
            )
            Text(
                text = fcmTokenRegistrationStatus,
                style = SSUType.Caption1Medium,
                color = N500,
            )

            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "FCM 수신 기록",
                style = SSUType.H5SemiBold,
            )
            Text(
                text = "최근 ${fcmRecords.size}개를 기기에 저장해요.",
                style = SSUType.Caption1Medium,
                color = N300,
            )
            OptionButton(
                text = "FCM 기록 지우기",
                onClick = onClearFcmHistory,
            )
            if (fcmRecords.isEmpty()) {
                Text(
                    text = "아직 수신된 FCM 기록이 없어요.",
                    style = SSUType.Caption1Medium,
                    color = N300,
                )
            } else {
                fcmRecords.take(20).forEach { record ->
                    FcmDebugRecordItem(record)
                }
            }
        }
    }
}

@Composable
private fun FcmTokenCard(token: String) {
    SelectionContainer {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(N100)
                .padding(14.dp),
            text = token,
            style = SSUType.Caption1Medium,
            color = N500,
        )
    }
}

@Composable
private fun FcmDebugRecordItem(record: FcmDebugRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(N100)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${record.receivedAt.toDebugTimeText()} · 상태=${record.status}",
            style = SSUType.H5SemiBold,
            color = record.status.toStatusColor(),
        )
        SelectionContainer {
            Text(
                text = "RemoteMessage\n${record.rawRemoteMessage.ifBlank { record.toRawRemoteMessageFallbackText() }}",
                style = SSUType.Caption1Medium,
                color = N500,
            )
        }
        Text(
            text = "처리 결과: handledAs=${record.handledAs.ifBlank { "-" }}, detail=${record.detail.ifBlank { "-" }}",
            style = SSUType.Caption1Medium,
            color = N500,
        )
    }
}

private fun String.toDebugTimeText(): String =
    runCatching {
        DEBUG_TIME_FORMATTER.format(
            Instant.parse(this).atZone(ZoneId.of("Asia/Seoul"))
        )
    }.getOrElse { this.ifBlank { "-" } }

private fun FcmDebugRecord.toRawRemoteMessageFallbackText(): String = buildString {
    appendLine("from=${from.ifBlank { "-" }}")
    appendLine("messageId=${messageId.ifBlank { id.take(8) }}")
    appendLine("messageType=${messageType.ifBlank { "-" }}")
    appendLine("collapseKey=${collapseKey.ifBlank { "-" }}")
    appendLine("sentTime=$sentTimeMillis")
    appendLine("ttl=$ttlSeconds")
    appendLine("priority=${priority.ifBlank { "-" }}")
    appendLine("originalPriority=${originalPriority.ifBlank { "-" }}")
    appendLine("notification=${if (notificationTitle.isBlank() && notificationBody.isBlank()) "null" else "{title=${notificationTitle.quote()}, body=${notificationBody.quote()}}"}")
    append("data=${data.toRawMapText()}")
}

private fun Map<String, String>.toRawMapText(): String =
    if (isEmpty()) {
        "{}"
    } else {
        entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "${key.quote()}: ${value.quote()}"
        }
    }

private fun String.quote(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

private fun String.toStatusColor(): Color =
    when (this) {
        "success" -> Color(0xFF00875A)
        "failure" -> Color(0xFFB21E1B)
        "skipped" -> Color(0xFF8A5A00)
        else -> N500
    }

private val DEBUG_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/dd HH:mm:ss", Locale.KOREA)
