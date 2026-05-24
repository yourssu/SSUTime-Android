package com.yourssu.ssutime.v2.fcm

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.UUID

// FCM 수신 기록 디버그 스위치입니다. 배포 전에 false로 바꾸면 저장과 화면 노출이 함께 꺼집니다.
const val FCM_DEBUG_HISTORY_ENABLED = true

private const val MAX_FCM_DEBUG_RECORDS = 100
private const val TAG = "FcmDebugHistory"

val Context.fcmDebugHistoryDataStore: DataStore<FcmDebugHistoryData> by dataStore(
    fileName = "fcm_debug_history.json",
    serializer = FcmDebugHistorySerializer,
)

@Serializable
data class FcmDebugHistoryData(
    val records: List<FcmDebugRecord> = emptyList(),
)

@Serializable
data class FcmDebugRecord(
    val id: String = "",
    val receivedAt: String = "",
    val handledAt: String = "",
    val rawRemoteMessage: String = "",
    val messageId: String = "",
    val messageType: String = "",
    val from: String = "",
    val collapseKey: String = "",
    val sentTimeMillis: Long = 0L,
    val originalPriority: String = "",
    val priority: String = "",
    val ttlSeconds: Int = 0,
    val handledAs: String = "",
    val status: String = "",
    val detail: String = "",
    val notificationTitle: String = "",
    val notificationBody: String = "",
    val data: Map<String, String> = emptyMap(),
)

object FcmDebugHistorySerializer : Serializer<FcmDebugHistoryData> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: FcmDebugHistoryData = FcmDebugHistoryData()

    override suspend fun readFrom(input: InputStream): FcmDebugHistoryData =
        try {
            json.decodeFromString<FcmDebugHistoryData>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("FCM 디버그 기록을 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: FcmDebugHistoryData, output: OutputStream) {
        output.write(
            json.encodeToString(FcmDebugHistoryData.serializer(), t)
                .encodeToByteArray()
        )
    }
}

class FcmDebugHistoryRepository(
    private val dataStore: DataStore<FcmDebugHistoryData>,
) {
    val history: Flow<FcmDebugHistoryData> = dataStore.data

    suspend fun recordReceived(
        message: RemoteMessage,
        handledAs: String,
    ): String? {
        if (!FCM_DEBUG_HISTORY_ENABLED) {
            return null
        }

        val record = message.toDebugRecord(handledAs)
        dataStore.updateData { current ->
            current.copy(
                records = (listOf(record) + current.records)
                    .take(MAX_FCM_DEBUG_RECORDS)
            )
        }
        Log.i(TAG, "FCM received: ${record.toLogText()}")
        return record.id
    }

    suspend fun markHandled(
        recordId: String?,
        status: String,
        detail: String,
    ) {
        if (!FCM_DEBUG_HISTORY_ENABLED || recordId.isNullOrBlank()) {
            return
        }

        dataStore.updateData { current ->
            current.copy(
                records = current.records.map { record ->
                    if (record.id == recordId) {
                        record.copy(
                            handledAt = Instant.now().toString(),
                            status = status,
                            detail = detail,
                        )
                    } else {
                        record
                    }
                }
            )
        }
        Log.i(TAG, "FCM handled: status=$status, detail=$detail")
    }

    suspend fun clear() {
        if (!FCM_DEBUG_HISTORY_ENABLED) {
            return
        }

        dataStore.updateData { FcmDebugHistoryData() }
    }
}

private fun RemoteMessage.toDebugRecord(handledAs: String): FcmDebugRecord {
    val notification = notification
    return FcmDebugRecord(
        id = UUID.randomUUID().toString(),
        receivedAt = Instant.now().toString(),
        rawRemoteMessage = toRawRemoteMessageText(),
        messageId = messageId.orEmpty(),
        messageType = messageType.orEmpty(),
        from = from.orEmpty(),
        collapseKey = collapseKey.orEmpty(),
        sentTimeMillis = sentTime,
        originalPriority = originalPriority.toPriorityText(),
        priority = priority.toPriorityText(),
        ttlSeconds = ttl,
        handledAs = handledAs,
        status = "received",
        notificationTitle = notification?.title.orEmpty(),
        notificationBody = notification?.body.orEmpty(),
        data = data.toSortedMap(),
    )
}

private fun RemoteMessage.toRawRemoteMessageText(): String {
    val notification = notification
    return buildString {
        appendLine("from=${from.orEmpty().ifBlank { "-" }}")
        appendLine("messageId=${messageId.orEmpty().ifBlank { "-" }}")
        appendLine("messageType=${messageType.orEmpty().ifBlank { "-" }}")
        appendLine("collapseKey=${collapseKey.orEmpty().ifBlank { "-" }}")
        appendLine("sentTime=$sentTime")
        appendLine("ttl=$ttl")
        appendLine("priority=${priority.toPriorityText()}")
        appendLine("originalPriority=${originalPriority.toPriorityText()}")
        appendLine(
            "notification=${if (notification == null) "null" else "{title=${notification.title.orEmpty().quote()}, body=${notification.body.orEmpty().quote()}}"}"
        )
        append("data=${data.toSortedMap().toRawMapText()}")
    }
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

private fun Int.toPriorityText(): String =
    when (this) {
        RemoteMessage.PRIORITY_HIGH -> "high"
        RemoteMessage.PRIORITY_NORMAL -> "normal"
        RemoteMessage.PRIORITY_UNKNOWN -> "unknown"
        else -> toString()
    }

private fun FcmDebugRecord.toLogText(): String =
    "id=$id, messageId=$messageId, handledAs=$handledAs, from=$from, data=$data"
