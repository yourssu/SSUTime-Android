package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.yourssu.data.AlertData
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

val Context.notificationStore: DataStore<AlertData> by dataStore(
    fileName = "notification.json",
    serializer = AlertDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { AlertDataSerializer.defaultValue },
)

private val notificationJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

object AlertDataSerializer : Serializer<AlertData> {
    override val defaultValue: AlertData = AlertData(
        valid = false,
        allowSystemAlert = false,
        allowCallAlert = false,
        callingAlertThresholdMinutes = 60,
        lastCallAlertRemindPeriod = null,
    )
    override suspend fun readFrom(input: InputStream): AlertData =
        try {
            notificationJson.decodeFromString<AlertData>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            SentryExceptionReporter.capture(serialization)
            throw CorruptionException("알림 정보를 읽어오지 못했습니다.", serialization)
        } catch (illegalArgument: IllegalArgumentException) {
            SentryExceptionReporter.capture(illegalArgument)
            throw CorruptionException("알림 정보 형식이 올바르지 않습니다.", illegalArgument)
        }

    override suspend fun writeTo(t: AlertData, output: OutputStream) {
        output.write(
            notificationJson.encodeToString(AlertData.serializer(), t)
                .encodeToByteArray()
        )
    }
}
