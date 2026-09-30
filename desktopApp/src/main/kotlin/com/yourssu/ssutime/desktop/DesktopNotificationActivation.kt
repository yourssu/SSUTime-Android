package com.yourssu.ssutime.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.Duration
import java.util.UUID

@Serializable
internal data class NotificationTapPayload(
    val dDay: Int,
    val taskCount: Int,
    val notificationType: String,
    val taskType: String,
    val subjectName: String,
    val createdAt: String,
) {
    fun properties(): Map<String, Any> = mapOf(
        "d_day" to dDay, "notification_task_count" to taskCount,
        "notification_type" to notificationType, "task_type" to taskType,
        "subject_name" to subjectName,
    )
}

// URIs expose only a random local token; notification data stays in the user's data directory.
internal class DesktopNotificationActivation(
    private val directory: Path = applicationDataDirectory().resolve("notification-activation"),
) {
    fun register(notification: DeadlineNotification, now: Instant = Instant.now()): String {
        Files.createDirectories(directory)
        val token = UUID.randomUUID().toString()
        val payload = NotificationTapPayload(notification.dDay, notification.notificationTaskCount,
            notification.notificationType, notification.representativeTodo?.type?.kor.orEmpty(),
            notification.representativeTodo?.subject?.name.orEmpty(), now.toString())
        Files.writeString(directory.resolve("$token.json"), Json.encodeToString(payload))
        cleanup(now)
        return "ssutime://notification/$token"
    }

    fun request(argument: String): Boolean = runCatching {
        val uri = URI(argument)
        if (uri.scheme != "ssutime" || uri.host != "notification" || uri.query != null || uri.fragment != null) return false
        val token = UUID.fromString(uri.path.removePrefix("/")).toString()
        if (uri.path != "/$token" || !Files.isRegularFile(directory.resolve("$token.json"))) return false
        // Publish before signalling the existing process; a random temp file avoids partial reads.
        val temporary = directory.resolve("${UUID.randomUUID()}.tmp")
        Files.writeString(temporary, token)
        Files.move(temporary, directory.resolve("$token.request"), StandardCopyOption.REPLACE_EXISTING)
        true
    }.getOrDefault(false)

    fun consume(now: Instant = Instant.now()): List<NotificationTapPayload> {
        if (!Files.isDirectory(directory)) return emptyList()
        val requests = Files.newDirectoryStream(directory, "*.request").use { it.toList() }
        return requests.mapNotNull { file ->
            runCatching {
                val token = UUID.fromString(file.fileName.toString().removeSuffix(".request")).toString()
                Files.deleteIfExists(file)
                val payload = Json.decodeFromString<NotificationTapPayload>(Files.readString(directory.resolve("$token.json")))
                payload.takeIf { Duration.between(Instant.parse(it.createdAt), now).let { age -> !age.isNegative && age <= Duration.ofDays(7) } }
            }.getOrNull()
        }
    }

    private fun cleanup(now: Instant) {
        Files.newDirectoryStream(directory, "*.json").use { files ->
            files.forEach { file ->
                if (Files.getLastModifiedTime(file).toInstant().isBefore(now.minus(Duration.ofDays(7)))) {
                    Files.deleteIfExists(file)
                }
            }
        }
    }
}
