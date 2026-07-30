package com.yourssu.ssutime.desktop

import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import com.yourssu.ssutime.desktop.core.model.dueDate
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DesktopDeadlineNotifier(
    private val onOpen: () -> Unit = {},
    private val onExit: () -> Unit = {},
) {
    private var trayIcon: TrayIcon? = null

    fun sendIfNeeded(
        todoData: AppTodoData,
        sentKeys: List<String>,
        now: Instant = Instant.now(),
    ): List<String> {
        val icon = ensureTrayIcon() ?: return emptyList()
        val reminders = todoData.todos
            .mapNotNull { todo -> todo.toReminder(now) }
            .filterNot { reminder -> reminder.key in sentKeys }
        if (reminders.isEmpty()) return emptyList()

        reminders.groupBy(Reminder::daysBefore).forEach { (daysBefore, group) ->
            if (daysBefore == 0L) {
                group.forEach { reminder ->
                    icon.displayMessage(
                        "오늘 마감, 아직 안 했죠?",
                        "${reminder.todo.displayName()} 오늘 마감이에요!",
                        TrayIcon.MessageType.INFO,
                    )
                }
            } else {
                val sorted = group.sortedWith(
                    compareBy<Reminder> { reminder -> reminder.todo.dueDate }
                        .thenBy { reminder -> reminder.todo.subject?.name.orEmpty() }
                        .thenBy { reminder -> reminder.todo.title },
                )
                val first = sorted.first().todo.displayName()
                val message = if (group.size == 1) {
                    "$first 마감 D-${daysBefore}이에요!"
                } else {
                    "$first 외 ${group.size - 1}건, 곧 마감이에요!"
                }
                icon.displayMessage(
                    "마감이 다가오고 있어요!",
                    message,
                    TrayIcon.MessageType.INFO,
                )
            }
        }
        return reminders.map(Reminder::key)
    }

    fun close() {
        trayIcon?.let { icon ->
            runCatching { SystemTray.getSystemTray().remove(icon) }
        }
        trayIcon = null
    }

    private fun ensureTrayIcon(): TrayIcon? {
        trayIcon?.let { return it }
        if (!isSupported()) {
            return null
        }
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().use { graphics ->
            graphics.color = Color(0xFE, 0x2B, 0x27)
            graphics.fillRoundRect(2, 2, 28, 28, 8, 8)
            graphics.color = Color.WHITE
            graphics.fillRect(8, 15, 5, 5)
            graphics.fillRect(14, 10, 11, 10)
        }
        return runCatching {
            val popupMenu = PopupMenu().apply {
                add(
                    MenuItem("SSUTime 열기").apply {
                        addActionListener { onOpen() }
                    },
                )
                addSeparator()
                add(
                    MenuItem("종료").apply {
                        addActionListener { onExit() }
                    },
                )
            }
            TrayIcon(image, "SSUTime", popupMenu).also { icon ->
                icon.isImageAutoSize = true
                icon.addActionListener { onOpen() }
                SystemTray.getSystemTray().add(icon)
                trayIcon = icon
            }
        }.getOrNull()
    }

    companion object {
        fun isSupported(): Boolean =
            !GraphicsEnvironment.isHeadless() && SystemTray.isSupported()
    }
}

private data class Reminder(
    val todo: AppTodo,
    val daysBefore: Long,
    val key: String,
)

private val deadlineZoneId: ZoneId = ZoneId.of("Asia/Seoul")
private val refreshWindow: Duration = Duration.ofMinutes(15)

private fun AppTodo.toReminder(now: Instant): Reminder? {
    val dueAt = dueDate.toDeadlineInstantOrNull() ?: return null
    val dueDateInKorea = dueAt.atZone(deadlineZoneId).toLocalDate()
    val nowInKorea = now.atZone(deadlineZoneId)
    val daysBefore = ChronoUnit.DAYS.between(
        nowInKorea.toLocalDate(),
        dueDateInKorea,
    )
    if (daysBefore !in 0L..3L) return null
    val notifyAt = dueDateInKorea
        .minusDays(daysBefore)
        .atTime(if (daysBefore == 0L) 9 else 18, 0)
        .atZone(deadlineZoneId)
        .toInstant()
    val elapsed = Duration.between(notifyAt, now)
    if (elapsed.isNegative || elapsed > refreshWindow) return null
    return Reminder(
        todo = this,
        daysBefore = daysBefore,
        key = "$todoId|$dueDate|$daysBefore|${type.name}|${subject?.id}|$title",
    )
}

private fun String.toDeadlineInstantOrNull(): Instant? = runCatching {
    ZonedDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME).toInstant()
}.recoverCatching {
    Instant.parse(this)
}.getOrNull()

private fun AppTodo.displayName(): String =
    listOf(subject?.name.orEmpty(), type.kor)
        .filter(String::isNotBlank)
        .joinToString(" ")
        .ifBlank { title.ifBlank { "과제" } }

private inline fun <T : java.awt.Graphics2D> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}
