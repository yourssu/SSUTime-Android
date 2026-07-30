package com.yourssu.ssutime.desktop.ui.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val deadlineZoneId: ZoneId = ZoneId.of("Asia/Seoul")

fun currentEpochMilliseconds(): Long = System.currentTimeMillis()

fun remainingSeconds(
    targetTime: String,
    nowEpochMilliseconds: Long = currentEpochMilliseconds(),
): Long = ChronoUnit.SECONDS.between(
    Instant.ofEpochMilli(nowEpochMilliseconds),
    targetTime.toDeadlineInstant(),
).coerceAtLeast(0L)

fun remainingDays(
    targetTime: String,
    nowEpochMilliseconds: Long = currentEpochMilliseconds(),
): Long = (remainingSeconds(targetTime, nowEpochMilliseconds) / 86_400L)
    .coerceAtLeast(0L)

fun remainingTimeText(
    targetTime: String,
    nowEpochMilliseconds: Long = currentEpochMilliseconds(),
): String {
    val seconds = remainingSeconds(targetTime, nowEpochMilliseconds).coerceAtLeast(0L)
    if (seconds < 60L) {
        return "${seconds}초"
    }
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    return hours.toString().padStart(2, '0') + ":" +
        minutes.toString().padStart(2, '0')
}

fun formatMonthDay(targetTime: String): String =
    targetTime.toDeadlineInstant()
        .atZone(deadlineZoneId)
        .format(
            DateTimeFormatter.ofPattern(
                if (Locale.getDefault().language == Locale.KOREAN.language) {
                    "MM월 dd일"
                } else {
                    "MMM dd"
                },
                Locale.getDefault(),
            ),
        )

fun formatMonthDayWithTime(targetTime: String): String =
    targetTime.toDeadlineInstant()
        .atZone(deadlineZoneId)
        .format(
            DateTimeFormatter.ofPattern(
                if (Locale.getDefault().language == Locale.KOREAN.language) {
                    "MM월 dd일 HH:mm:ss"
                } else {
                    "MMM dd HH:mm:ss"
                },
                Locale.getDefault(),
            ),
        )

fun formatUpdatedTime(targetTime: String): String =
    Instant.parse(targetTime)
        .atZone(deadlineZoneId)
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

private fun String.toDeadlineInstant(): Instant {
    val parsed = DateTimeFormatter.ISO_DATE_TIME.parseBest(
        this,
        ZonedDateTime::from,
        OffsetDateTime::from,
        LocalDateTime::from,
    )
    return when (parsed) {
        is ZonedDateTime -> parsed.toInstant()
        is OffsetDateTime -> parsed.toInstant()
        is LocalDateTime -> parsed.atZone(deadlineZoneId).toInstant()
        else -> error("지원하지 않는 마감 시각 형식입니다: $this")
    }
}
