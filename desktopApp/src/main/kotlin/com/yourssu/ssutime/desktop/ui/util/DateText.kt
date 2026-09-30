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
): Long = remainingDaysUntilDeadline(targetTime, Instant.ofEpochMilli(nowEpochMilliseconds))

fun remainingTimeText(
    targetTime: String,
    nowEpochMilliseconds: Long = currentEpochMilliseconds(),
): String {
    return remainingTimeTextUntilDeadline(targetTime, Instant.ofEpochMilli(nowEpochMilliseconds))
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

fun formatMonthDayWithTime(targetTime: String, includeSeconds: Boolean = true): String =
    targetTime.toDeadlineInstant()
        .atZone(deadlineZoneId)
        .format(
            DateTimeFormatter.ofPattern(
                if (Locale.getDefault().language == Locale.KOREAN.language) {
                    if (includeSeconds) "MM월 dd일 HH:mm:ss" else "MM월 dd일 HH:mm"
                } else {
                    if (includeSeconds) "MMM dd HH:mm:ss" else "MMM dd HH:mm"
                },
                Locale.getDefault(),
            ),
        )

fun formatUpdatedTime(targetTime: String): String =
    Instant.parse(targetTime)
        .atZone(deadlineZoneId)
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

internal fun String.toDeadlineInstant(): Instant = toTodoDeadlineInstant()
