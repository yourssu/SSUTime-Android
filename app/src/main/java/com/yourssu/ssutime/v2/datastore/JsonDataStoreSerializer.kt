package com.yourssu.ssutime.v2.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Shared JSON-backed DataStore serializer that maps [SerializationException]
 * into [CorruptionException] while reporting failures to Sentry.
 */
open class JsonDataStoreSerializer<T>(
    override val defaultValue: T,
    private val kSerializer: KSerializer<T>,
    private val readErrorMessage: String,
    private val json: Json = DefaultJsonDataStoreFormat,
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T =
        try {
            json.decodeFromString(kSerializer, input.readBytes().decodeToString())
        } catch (serialization: SerializationException) {
            SentryExceptionReporter.capture(serialization)
            throw CorruptionException(readErrorMessage, serialization)
        } catch (illegalArgument: IllegalArgumentException) {
            SentryExceptionReporter.capture(illegalArgument)
            throw CorruptionException(readErrorMessage, illegalArgument)
        }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(json.encodeToString(kSerializer, t).encodeToByteArray())
    }
}

private val DefaultJsonDataStoreFormat = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}
