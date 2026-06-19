package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.yourssu.data.LocalTimetable
import com.yourssu.data.LocalTimetableCell
import io.github.chlwhdtn03.data.Lms.Timetable
import io.github.chlwhdtn03.data.Lms.TimetableCell
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import io.github.chlwhdtn03.data.Lms.DayOfWeek as LmsDayOfWeek

val Context.timetableDataStore: DataStore<LocalTimetable> by dataStore(
    fileName = "timetable.json",
    serializer = TimetableDataSerializer,
)

object TimetableDataSerializer : Serializer<LocalTimetable> {
    override val defaultValue: LocalTimetable = LocalTimetable()
    override suspend fun readFrom(input: InputStream): LocalTimetable =
        try {
            Json.decodeFromString<LocalTimetable>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("시간표 정보를 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: LocalTimetable, output: OutputStream) {
        output.write(
            Json.encodeToString(LocalTimetable.serializer(), t)
                .encodeToByteArray()
        )
    }
}

fun Timetable.toLocal(): LocalTimetable {
    return LocalTimetable(
        year = this.year,
        semester = this.semester,
        items = this.items.map { it.toLocal() }
    )
}

fun TimetableCell.toLocal(): LocalTimetableCell {
    return LocalTimetableCell(
        dayOfWeek = this.dayOfWeek.name,
        period = this.period,
        periodTime = this.periodTime,
        subject = this.subject,
        professor = this.professor,
        time = this.time,
        classroom = this.classroom
    )
}

fun LocalTimetable.toDomain(): Timetable {
    return Timetable(
        year = this.year,
        semester = this.semester,
        items = this.items.map { it.toDomain() }
    )
}

fun LocalTimetableCell.toDomain(): TimetableCell {
    return TimetableCell(
        dayOfWeek = runCatching { LmsDayOfWeek.valueOf(this.dayOfWeek) }.getOrDefault(LmsDayOfWeek.MONDAY),
        period = this.period,
        periodTime = this.periodTime,
        subject = this.subject,
        professor = this.professor,
        time = this.time,
        classroom = this.classroom
    )
}
