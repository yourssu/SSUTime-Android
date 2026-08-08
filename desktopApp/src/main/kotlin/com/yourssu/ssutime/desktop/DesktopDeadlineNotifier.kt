package com.yourssu.ssutime.desktop

import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import com.yourssu.ssutime.desktop.core.model.dueDate
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
        )
        if (notifications.isEmpty()) return emptyList()

        return if (sendWindowsToasts(notifications)) {
            notifications.flatMap(DeadlineNotification::reminderKeys)
        } else {
            emptyList()
        }
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
)

private val deadlineZoneId: ZoneId = ZoneId.of("Asia/Seoul")
private val refreshWindow: Duration = Duration.ofMinutes(15)
private const val ssuTimeAumid = "Campo.1711AB9C2595_200qmz0tpjzc8!SSUTime"
internal const val windowsToastXmlTemplate =
    "<toast launch=\"--notification-activated\">" +
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
        val payloadJson = Json.encodeToString(
            notifications.map { notification ->
                WindowsToastPayload(notification.title, notification.body)
            },
        )
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
