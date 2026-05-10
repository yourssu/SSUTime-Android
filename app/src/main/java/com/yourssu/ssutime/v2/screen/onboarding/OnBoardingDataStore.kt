package com.yourssu.ssutime.v2.screen.onboarding

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

val Context.onBoardingDataStore: DataStore<OnBoardingData> by dataStore(
    fileName = "onboarding.json",
    serializer = OnBoardingDataSerializer,
)

object OnBoardingDataSerializer : Serializer<OnBoardingData> {
    override val defaultValue: OnBoardingData = OnBoardingData()
    override suspend fun readFrom(input: InputStream): OnBoardingData =
        try {
            Json.decodeFromString<OnBoardingData>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("온보딩 정보를 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: OnBoardingData, output: OutputStream) {
        output.write(
            Json.encodeToString(OnBoardingData.serializer(), t)
                .encodeToByteArray()
        )
    }
}
