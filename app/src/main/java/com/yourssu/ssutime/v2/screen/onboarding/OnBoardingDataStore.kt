package com.yourssu.ssutime.v2.screen.onboarding

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

val Context.onBoardingDataStore: DataStore<OnBoardingData> by dataStore(
    fileName = "onboarding.json",
    serializer = OnBoardingDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { OnBoardingDataSerializer.defaultValue },
)

private val onBoardingJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

object OnBoardingDataSerializer : Serializer<OnBoardingData> {
    override val defaultValue: OnBoardingData = OnBoardingData()
    override suspend fun readFrom(input: InputStream): OnBoardingData =
        try {
            onBoardingJson.decodeFromString<OnBoardingData>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            SentryExceptionReporter.capture(serialization)
            throw CorruptionException("온보딩 정보를 읽어오지 못했습니다.", serialization)
        } catch (illegalArgument: IllegalArgumentException) {
            SentryExceptionReporter.capture(illegalArgument)
            throw CorruptionException("온보딩 정보 형식이 올바르지 않습니다.", illegalArgument)
        }

    override suspend fun writeTo(t: OnBoardingData, output: OutputStream) {
        output.write(
            onBoardingJson.encodeToString(OnBoardingData.serializer(), t)
                .encodeToByteArray()
        )
    }
}
