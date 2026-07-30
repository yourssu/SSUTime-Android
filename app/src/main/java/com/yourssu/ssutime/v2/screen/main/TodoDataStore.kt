package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.yourssu.data.TodoData
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

val Context.todoDataStore: DataStore<TodoData> by dataStore(
    fileName = "todos.json",
    serializer = TodoDataSerializer,
)

object TodoDataSerializer : Serializer<TodoData> {
    override val defaultValue: TodoData = TodoData()
    override suspend fun readFrom(input: InputStream): TodoData =
        try {
            Json.decodeFromString<TodoData>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            SentryExceptionReporter.capture(serialization)
            throw CorruptionException("과제 정보를 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: TodoData, output: OutputStream) {
        output.write(
            Json.encodeToString(TodoData.serializer(), t)
                .encodeToByteArray()
        )
    }
}
