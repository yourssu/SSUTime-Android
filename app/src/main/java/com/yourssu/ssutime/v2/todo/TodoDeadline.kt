package com.yourssu.ssutime.v2.todo

import com.yourssu.data.TodoInfo
import java.text.Collator
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

internal val TODO_DEADLINE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

internal fun String.toTodoDeadlineInstant(): Instant = toTodoDeadlineInstantOrNull()
    ?: error("Unsupported target time format: $this")

internal fun String.toTodoDeadlineInstantOrNull(): Instant? = runCatching {
    val parsedTime = DateTimeFormatter.ISO_DATE_TIME.parseBest(
        this,
        ZonedDateTime::from,
        OffsetDateTime::from,
        LocalDateTime::from,
    )

    when (parsedTime) {
        is ZonedDateTime -> parsedTime.toInstant()
        is OffsetDateTime -> parsedTime.toInstant()
        is LocalDateTime -> parsedTime.atZone(TODO_DEADLINE_ZONE_ID).toInstant()
        else -> error("Unsupported target time format: $this")
    }
}.getOrNull()

internal fun remainingDaysUntilDeadline(
    targetTime: String,
    now: Instant = Instant.now(),
): Long {
    val targetInstant = targetTime.toTodoDeadlineInstant()
    return max(0, ChronoUnit.DAYS.between(now, targetInstant))
}

internal fun remainingTimeTextUntilDeadline(
    targetTime: String,
    now: Instant = Instant.now(),
): String {
    val targetInstant = targetTime.toTodoDeadlineInstant()
    val remainingSeconds = max(0, ChronoUnit.SECONDS.between(now, targetInstant))

    return if (remainingSeconds < 60) {
        "${remainingSeconds}초"
    } else {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60

        "%02d:%02d".format(hours, minutes)
    }
}

internal fun List<TodoInfo>.sortedByDeadlineThenName(): List<TodoInfo> =
    sortedWith(todoDeadlineThenNameComparator())

internal fun compareTodosByDeadlineThenName(left: TodoInfo, right: TodoInfo): Int =
    todoDeadlineThenNameComparator().compare(left, right)

private fun todoDeadlineThenNameComparator(): Comparator<TodoInfo> {
    val koreanCollator = Collator.getInstance(Locale.KOREAN)

    return Comparator { left, right ->
        compareDeadline(left.due_date, right.due_date)
            .takeIf { it != 0 }
            ?: koreanCollator.compare(left.sortName(), right.sortName())
                .takeIf { it != 0 }
            ?: koreanCollator.compare(left.title, right.title)
                .takeIf { it != 0 }
            ?: left.todoId.compareTo(right.todoId)
    }
}

private fun compareDeadline(left: String, right: String): Int {
    val leftInstant = left.toTodoDeadlineInstantOrNull()
    val rightInstant = right.toTodoDeadlineInstantOrNull()

    return when {
        leftInstant != null && rightInstant != null -> leftInstant.compareTo(rightInstant)
        leftInstant != null -> -1
        rightInstant != null -> 1
        else -> left.compareTo(right)
    }
}

private fun TodoInfo.sortName(): String =
    subject?.name
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: title.trim()
