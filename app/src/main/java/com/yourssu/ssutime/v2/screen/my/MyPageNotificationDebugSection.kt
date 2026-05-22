package com.yourssu.ssutime.v2.screen.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
            text = "${record.receivedAt.toDebugTimeText()} · ${record.status}",
            style = SSUType.H5SemiBold,
            color = record.status.toStatusColor(),
        )
        Text(
            text = "서버 Push: ${record.toServerPushLabel()}",
            style = SSUType.Caption1Medium,
            color = N500,
        )
        Text(
            text = "처리=${record.handledAs.ifBlank { "-" }}, type=${record.serverTypeOrPayload().ifBlank { "-" }}, action=${record.action.ifBlank { "-" }}",
            style = SSUType.Caption1Medium,
            color = N500,
        )
        Text(
            text = "messageId=${record.messageId.ifBlank { record.id.take(8) }}",
            style = SSUType.Caption1Medium,
            color = N500,
        )
        Text(
            text = "from=${record.from.ifBlank { "-" }}, priority=${record.priority.ifBlank { "-" }}, ttl=${record.ttlSeconds}s",
            style = SSUType.Caption1Medium,
            color = N500,
        )
        if (record.serverRequestIdOrPayload().isNotBlank() || record.serverSentAtOrPayload().isNotBlank()) {
            Text(
                text = "serverRequest=${record.serverRequestIdOrPayload().ifBlank { "-" }}, serverSentAt=${record.serverSentAtOrPayload().toServerSentAtText()}",
                style = SSUType.Caption1Medium,
                color = N500,
            )
        }
        if (record.detail.isNotBlank()) {
            Text(
                text = record.detail,
                style = SSUType.Caption1Medium,
                color = N500,
            )
        }
        if (record.notificationTitle.isNotBlank() || record.notificationBody.isNotBlank()) {
            Text(
                text = "notification=${listOf(record.notificationTitle, record.notificationBody).filter { it.isNotBlank() }.joinToString(" / ")}",
                style = SSUType.Caption1Medium,
                color = N500,
            )
        }
        Text(
            text = "data=${record.data.toPayloadText()}",
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

private fun Map<String, String>.toPayloadText(): String =
    if (isEmpty()) {
        "-"
    } else {
        entries.joinToString(", ") { "${it.key}=${it.value}" }
    }

private fun FcmDebugRecord.toServerPushLabel(): String {
    val type = serverTypeOrPayload()
    val reason = serverReasonOrPayload()
    return when {
        type.isNotBlank() && reason.isNotBlank() -> "$type ($reason)"
        type.isNotBlank() -> type
        action.isNotBlank() -> "action=$action"
        data.isNotEmpty() -> "data-only"
        notificationTitle.isNotBlank() || notificationBody.isNotBlank() -> "notification"
        else -> "-"
    }
}

private fun FcmDebugRecord.serverTypeOrPayload(): String = serverType.ifBlank { data["type"].orEmpty() }

private fun FcmDebugRecord.serverReasonOrPayload(): String = serverReason.ifBlank { data["reason"].orEmpty() }

private fun FcmDebugRecord.serverRequestIdOrPayload(): String =
    serverRequestId.ifBlank { data["requestId"].orEmpty() }

private fun FcmDebugRecord.serverSentAtOrPayload(): String =
    serverSentAt.ifBlank { data["sentAt"].orEmpty() }

private fun String.toServerSentAtText(): String {
    if (isBlank()) return "-"

    val epochMillis = toLongOrNull()
    if (epochMillis != null) {
        return runCatching {
            DEBUG_TIME_FORMATTER.format(
                Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("Asia/Seoul"))
            )
        }.getOrElse { this }
    }

    return runCatching {
        DEBUG_TIME_FORMATTER.format(
            Instant.parse(this).atZone(ZoneId.of("Asia/Seoul"))
        )
    }.getOrElse { this }
}

private fun String.toStatusColor(): Color =
    when (this) {
        "success" -> Color(0xFF00875A)
        "failure" -> Color(0xFFB21E1B)
        "skipped" -> Color(0xFF8A5A00)
        else -> N500
    }

private val DEBUG_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/dd HH:mm:ss", Locale.KOREA)
