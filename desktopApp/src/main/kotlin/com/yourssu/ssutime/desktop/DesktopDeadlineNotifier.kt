package com.yourssu.ssutime.desktop

import com.yourssu.ssutime.desktop.analytics.DesktopAnalytics
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import com.yourssu.ssutime.desktop.core.model.dueDate
import com.yourssu.ssutime.desktop.ui.util.toTodoDeadlineInstantOrNull
import com.yourssu.data.NewTodoNotificationRecord
import com.yourssu.data.TodoType
import com.yourssu.ssutime.desktop.ui.resources.*
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class DesktopDeadlineNotifier(
    private val onOpen: () -> Unit = {},
    private val onExit: () -> Unit = {},
) {
    private var trayIcon: TrayIcon? = null

    fun start(): Boolean = ensureTrayIcon() != null

    suspend fun sendIfNeeded(
        todoData: AppTodoData,
        sentKeys: List<String>,
        now: Instant = Instant.now(),
    ): List<String> {
        val notifications = buildAutomaticDeadlineNotifications(
            todoData = todoData,
            sentKeys = sentKeys,
            now = now,
        ).map { it.withLocalizedDeadlineText() }
        if (notifications.isEmpty()) return emptyList()

        return if (sendWindowsToasts(notifications)) {
            notifications.forEach { notification ->
                notification.representativeTodo?.let { repTodo ->
                    DesktopAnalytics.notificationReceived(
                        dDay = notification.dDay,
                        notificationTaskCount = notification.notificationTaskCount,
                        representativeTodo = repTodo,
                        notificationType = notification.notificationType,
                    )
                }
            }
            notifications.flatMap(DeadlineNotification::reminderKeys)
        } else {
            emptyList()
        }
    }

    internal suspend fun sendNewTodosIfNeeded(todoData: AppTodoData, now: Instant = Instant.now()): NewTodoAnnouncement? {
        val result = buildNewTodoAnnouncement(todoData, now) ?: return null
        val first = result.records.first()
        val subject = first.subjectName.ifBlank { getString(Res.string.new_todo_default_subject) }
        val type = localizedTodoType(first.type)
        val body = if (result.records.size == 1) {
            getString(Res.string.new_todo_notification_single, subject, type)
        } else {
            getString(Res.string.new_todo_notification_multiple, subject, type, result.records.size - 1)
        }
        val localized = result.copy(notification = result.notification.copy(
            title = getString(Res.string.new_todo_notification_title),
            body = body,
        ))
        return localized.takeIf { sendWindowsToasts(listOf(it.notification)) }
    }

    private suspend fun DeadlineNotification.withLocalizedDeadlineText(): DeadlineNotification {
        val todo = representativeTodo ?: return this
        val itemName = listOf(todo.subject?.name.orEmpty(), localizedTodoType(todo.type))
            .filter(String::isNotBlank).joinToString(" ")
            .ifBlank { todo.title.ifBlank { getString(Res.string.deadline_default_item) } }
        val message = when {
            dDay == 0 -> getString(Res.string.deadline_today_message, itemName)
            notificationTaskCount == 1 -> getString(Res.string.deadline_d_day_message, itemName, dDay)
            else -> getString(Res.string.deadline_multiple_message, itemName, notificationTaskCount - 1)
        }
        return copy(
            title = getString(if (dDay == 0) Res.string.deadline_title_today else Res.string.deadline_title_upcoming),
            body = message,
        )
    }

    private suspend fun localizedTodoType(type: TodoType): String = getString(when (type) {
        TodoType.COMMONS -> Res.string.todo_type_lecture
        TodoType.QUIZ -> Res.string.todo_type_quiz
        TodoType.SUBMITTED -> Res.string.todo_type_submitted
        TodoType.SUBMITTED_LATE -> Res.string.todo_type_submitted_late
        else -> Res.string.todo_type_assignment
    })

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
        return createTrayIcon(loadCheckboxIcon())
    }

    private fun loadCheckboxIcon(): BufferedImage {
        javaClass.getResourceAsStream("/icons/checkbox.png")?.use { stream ->
            ImageIO.read(stream)?.let { return it }
        }
        return BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB).also { image ->
            image.createGraphics().use { graphics ->
                graphics.color = Color(0xFE, 0x4F, 0x4C)
                graphics.fillRoundRect(0, 0, 32, 32, 11, 11)
                graphics.color = Color.WHITE
                graphics.stroke = java.awt.BasicStroke(
                    3.155f,
                    java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND,
                )
                graphics.drawPolyline(
                    intArrayOf(9, 14, 24),
                    intArrayOf(16, 21, 11),
                    3,
                )
            }
        }
    }

    private fun createTrayIcon(image: BufferedImage): TrayIcon? {
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

internal data class DeadlineNotification(
    val title: String,
    val body: String,
    val reminderKeys: List<String>,
    val dDay: Int = 0,
    val notificationTaskCount: Int = 1,
    val representativeTodo: AppTodo? = null,
    val notificationType: String = "deadline_soon",
)

internal data class NewTodoAnnouncement(
    val notification: DeadlineNotification,
    val cutoff: Instant,
    val records: List<NewTodoNotificationRecord>,
) {
    val consumedKeys: List<String> get() = records.map { it.todoKey }
}

internal fun buildNewTodoAnnouncement(todoData: AppTodoData, now: Instant): NewTodoAnnouncement? {
    val localNow = now.atZone(ZoneId.of("Asia/Seoul"))
    val todayCutoff = localNow.toLocalDate().atTime(18, 0).atZone(ZoneId.of("Asia/Seoul"))
    val cutoff = (if (todayCutoff.toInstant().isAfter(now)) todayCutoff.minusDays(1) else todayCutoff).toInstant()
    val lastAnnouncement = runCatching { Instant.parse(todoData.lastNewTodoAnnouncementAt) }.getOrNull()
    if (lastAnnouncement != null && !lastAnnouncement.isBefore(cutoff)) return null

    // Match Android: missed records survive until delivery; cutoff-time records wait for the next batch.
    val ready = todoData.pendingNewTodoNotifications.filter {
        val discovered = runCatching { Instant.parse(it.discoveredAt) }.getOrNull()
        it.type in setOf(TodoType.ASSIGNMENT, TodoType.COMMONS, TodoType.QUIZ) &&
            discovered?.isBefore(cutoff) == true
    }
    val first = ready.firstOrNull() ?: return null
    val subject = first.subjectName.ifBlank { "공통" }
    val type = when (first.type) {
        TodoType.COMMONS -> "강의"
        TodoType.QUIZ -> "퀴즈"
        else -> "과제"
    }
    val body = if (ready.size == 1) "$subject ${type}가 새로 올라왔어요." else "$subject $type 외 ${ready.size - 1}건이 올라왔어요."
    return NewTodoAnnouncement(
        DeadlineNotification("새 할 일이 올라왔어요", body, emptyList(), notificationType = "new_todo"),
        cutoff,
        ready,
    )
}

internal fun AppTodoData.withSentNewTodoAnnouncement(batch: NewTodoAnnouncement): AppTodoData = copy(
    lastNewTodoAnnouncementAt = batch.cutoff.toString(),
    pendingNewTodoNotifications = pendingNewTodoNotifications.filterNot { it in batch.records.toSet() },
)

private data class Reminder(
    val todo: AppTodo,
    val daysBefore: Long,
    val key: String,
)

@Serializable
private data class WindowsToastPayload(
    val title: String,
    val body: String,
    val activationUri: String,
)

private val deadlineZoneId: ZoneId = ZoneId.of("Asia/Seoul")
private val refreshWindow: Duration = Duration.ofMinutes(15)
private const val ssuTimeAumid = "Campo.1711AB9C2595_200qmz0tpjzc8!SSUTime"
internal const val windowsToastXmlTemplate =
    "<toast activationType=\"protocol\" launch=\"ssutime://notification\">" +
        "<visual><binding template=\"ToastGeneric\">" +
        "<text></text><text></text>" +
        "</binding></visual></toast>"
private val windowsToastCommand: String by lazy {
    val script = """
        ${'$'}ErrorActionPreference = 'Stop'
        Add-Type -AssemblyName System.Runtime.WindowsRuntime
        [void][Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType=WindowsRuntime]
        [void][Windows.UI.Notifications.ToastNotification, Windows.UI.Notifications, ContentType=WindowsRuntime]
        [void][Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType=WindowsRuntime]
        ${'$'}payloadBase64 = [Console]::In.ReadToEnd()
        ${'$'}payloadJson = [Text.Encoding]::UTF8.GetString(
            [Convert]::FromBase64String(${'$'}payloadBase64)
        )
        ${'$'}items = @((ConvertFrom-Json ${'$'}payloadJson))
        ${'$'}notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('$ssuTimeAumid')
        if (${'$'}notifier.Setting.ToString() -ne 'Enabled') { exit 3 }
        foreach (${'$'}item in ${'$'}items) {
            ${'$'}xml = [Windows.Data.Xml.Dom.XmlDocument]::new()
            ${'$'}xml.LoadXml(
                '$windowsToastXmlTemplate'
            )
            [void]${'$'}xml.DocumentElement.SetAttribute('launch', [string]${'$'}item.activationUri)
            ${'$'}texts = ${'$'}xml.GetElementsByTagName('text')
            [void]${'$'}texts.Item(0).AppendChild(${'$'}xml.CreateTextNode([string]${'$'}item.title))
            [void]${'$'}texts.Item(1).AppendChild(${'$'}xml.CreateTextNode([string]${'$'}item.body))
            ${'$'}toast = [Windows.UI.Notifications.ToastNotification]::new(${'$'}xml)
            ${'$'}notifier.Show(${'$'}toast)
        }
    """.trimIndent()
    Base64.getEncoder().encodeToString(script.toByteArray(StandardCharsets.UTF_16LE))
}

internal fun buildAutomaticDeadlineNotifications(
    todoData: AppTodoData,
    sentKeys: List<String>,
    now: Instant,
): List<DeadlineNotification> {
    val reminders = todoData.todos
        .mapNotNull { todo -> todo.toReminder(now) }
        .filterNot { reminder -> reminder.key in sentKeys }

    return reminders.toDeadlineNotifications()
}

private fun List<Reminder>.toDeadlineNotifications(): List<DeadlineNotification> =
    groupBy(Reminder::daysBefore).flatMap { (daysBefore, group) ->
        if (daysBefore == 0L) {
            group.map { reminder ->
                DeadlineNotification(
                    title = "오늘 마감, 아직 안 했죠?",
                    body = "${reminder.todo.displayName()} 오늘 마감이에요!",
                    reminderKeys = listOf(reminder.key),
                    dDay = 0,
                    notificationTaskCount = 1,
                    representativeTodo = reminder.todo,
                    notificationType = "due_today",
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
            listOf(
                DeadlineNotification(
                    title = "마감이 다가오고 있어요!",
                    body = message,
                    reminderKeys = sorted.map(Reminder::key),
                    dDay = daysBefore.toInt(),
                    notificationTaskCount = group.size,
                    representativeTodo = sorted.first().todo,
                    notificationType = "deadline_soon",
                ),
            )
        }
    }

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

private suspend fun sendWindowsToasts(notifications: List<DeadlineNotification>): Boolean =
    withContext(Dispatchers.IO) {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            return@withContext false
        }
        val payloadJson = runCatching { Json.encodeToString(
            notifications.map { notification ->
                WindowsToastPayload(notification.title, notification.body, DesktopNotificationActivation().register(notification))
            },
        ) }.getOrElse { return@withContext false }
        val payloadBase64 = Base64.getEncoder().encodeToString(
            payloadJson.toByteArray(StandardCharsets.UTF_8),
        )
        runCatching {
            val windowsDirectory = System.getenv("SystemRoot") ?: "C:\\Windows"
            val powershell = Path.of(
                windowsDirectory,
                "System32",
                "WindowsPowerShell",
                "v1.0",
                "powershell.exe",
            ).toString()
            val process = ProcessBuilder(
                powershell,
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-WindowStyle",
                "Hidden",
                "-EncodedCommand",
                windowsToastCommand,
            )
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            process.outputStream.bufferedWriter(StandardCharsets.US_ASCII).use { writer ->
                writer.write(payloadBase64)
            }
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                false
            } else {
                process.exitValue() == 0
            }
        }.getOrDefault(false)
    }

private fun String.toDeadlineInstantOrNull(): Instant? = toTodoDeadlineInstantOrNull()

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
