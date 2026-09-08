package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.yourssu.data.LabsData
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

val Context.labsDataStore: DataStore<LabsData> by dataStore(
    fileName = "labs.json",
    serializer = LabsDataSerializer,
)

object LabsDataSerializer : Serializer<LabsData> {
    override val defaultValue: LabsData = LabsData()

    override suspend fun readFrom(input: InputStream): LabsData =
        try {
            Json.decodeFromString<LabsData>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            SentryExceptionReporter.capture(serialization)
            throw CorruptionException("실험실 정보를 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: LabsData, output: OutputStream) {
        output.write(
            Json.encodeToString(LabsData.serializer(), t)
                .encodeToByteArray()
        )
    }
}
