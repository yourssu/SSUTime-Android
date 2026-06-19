package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.yourssu.data.LocalGraduate
import com.yourssu.data.LocalGraduateCell
import com.yourssu.data.LocalScholarship
import com.yourssu.data.LocalScholarshipCell
import com.yourssu.data.LocalTuition
import com.yourssu.data.LocalTuitionCell
import io.github.chlwhdtn03.data.Lms.GraduateTable
import io.github.chlwhdtn03.data.Lms.GraduateTableCell
import io.github.chlwhdtn03.data.Lms.ScholarshipHistoryCell
import io.github.chlwhdtn03.data.Lms.ScholarshipHistoryTable
import io.github.chlwhdtn03.data.Lms.TuitionCell
import io.github.chlwhdtn03.data.Lms.TuitionTable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

// DataStore declarations
val Context.scholarshipDataStore: DataStore<LocalScholarship> by dataStore(
    fileName = "scholarship.json",
    serializer = ScholarshipDataSerializer,
)

val Context.tuitionDataStore: DataStore<LocalTuition> by dataStore(
    fileName = "tuition.json",
    serializer = TuitionDataSerializer,
)

val Context.graduateDataStore: DataStore<LocalGraduate> by dataStore(
    fileName = "graduate.json",
    serializer = GraduateDataSerializer,
)

// Serializers
object ScholarshipDataSerializer : Serializer<LocalScholarship> {
    override val defaultValue: LocalScholarship = LocalScholarship()
    override suspend fun readFrom(input: InputStream): LocalScholarship =
        try {
            Json.decodeFromString<LocalScholarship>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("장학 정보를 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: LocalScholarship, output: OutputStream) {
        output.write(
            Json.encodeToString(LocalScholarship.serializer(), t)
                .encodeToByteArray()
        )
    }
}

object TuitionDataSerializer : Serializer<LocalTuition> {
    override val defaultValue: LocalTuition = LocalTuition()
    override suspend fun readFrom(input: InputStream): LocalTuition =
        try {
            Json.decodeFromString<LocalTuition>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("등록금 정보를 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: LocalTuition, output: OutputStream) {
        output.write(
            Json.encodeToString(LocalTuition.serializer(), t)
                .encodeToByteArray()
        )
    }
}

object GraduateDataSerializer : Serializer<LocalGraduate> {
    override val defaultValue: LocalGraduate = LocalGraduate()
    override suspend fun readFrom(input: InputStream): LocalGraduate =
        try {
            Json.decodeFromString<LocalGraduate>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("졸업 사정 정보를 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: LocalGraduate, output: OutputStream) {
        output.write(
            Json.encodeToString(LocalGraduate.serializer(), t)
                .encodeToByteArray()
        )
    }
}

// Scholarship Mappings
fun ScholarshipHistoryTable.toLocal(): LocalScholarship {
    return LocalScholarship(
        items = this.items.map { it.toLocal() }
    )
}

fun ScholarshipHistoryCell.toLocal(): LocalScholarshipCell {
    return LocalScholarshipCell(
        year = this.year.orEmpty(),
        semester = this.semester.orEmpty(),
        scholarshipName = this.scholarshipName.orEmpty(),
        paymentMethod = this.paymentMethod.orEmpty(),
        processStatus = this.processStatus.orEmpty(),
        note = this.note.orEmpty(),
        dropReason = this.dropReason.orEmpty(),
        processDate = this.processDate.orEmpty(),
        selectedAmount = this.selectedAmount.orEmpty(),
        actualAmount = this.actualAmount.orEmpty(),
        redeemedAmount = this.redeemedAmount.orEmpty(),
        replacedAmount = this.replacedAmount.orEmpty(),
        replacedScholarshipName = this.replacedScholarshipName.orEmpty(),
        workDepartment = this.workDepartment.orEmpty()
    )
}

fun LocalScholarship.toDomain(): ScholarshipHistoryTable {
    return ScholarshipHistoryTable(
        items = this.items.map { it.toDomain() }
    )
}

fun LocalScholarshipCell.toDomain(): ScholarshipHistoryCell {
    return ScholarshipHistoryCell(
        year = this.year,
        semester = this.semester,
        scholarshipName = this.scholarshipName,
        paymentMethod = this.paymentMethod,
        processStatus = this.processStatus,
        note = this.note,
        dropReason = this.dropReason,
        processDate = this.processDate,
        selectedAmount = this.selectedAmount,
        actualAmount = this.actualAmount,
        redeemedAmount = this.redeemedAmount,
        replacedAmount = this.replacedAmount,
        replacedScholarshipName = this.replacedScholarshipName,
        workDepartment = this.workDepartment
    )
}

// Tuition Mappings
fun TuitionTable.toLocal(): LocalTuition {
    return LocalTuition(
        items = this.items.map { it.toLocal() }
    )
}

fun TuitionCell.toLocal(): LocalTuitionCell {
    return LocalTuitionCell(
        year = this.year.orEmpty(),
        semester = this.semester.orEmpty(),
        grade = this.grade.orEmpty(),
        registrationType = this.registrationType.orEmpty(),
        registrationDate = this.registrationDate.orEmpty(),
        amount = this.amount.orEmpty(),
        reduction = this.reduction.orEmpty(),
        paymentAmount = this.paymentAmount.orEmpty()
    )
}

fun LocalTuition.toDomain(): TuitionTable {
    return TuitionTable(
        items = this.items.map { it.toDomain() }
    )
}

fun LocalTuitionCell.toDomain(): TuitionCell {
    return TuitionCell(
        year = this.year,
        semester = this.semester,
        grade = this.grade,
        registrationType = this.registrationType,
        registrationDate = this.registrationDate,
        amount = this.amount,
        reduction = this.reduction,
        paymentAmount = this.paymentAmount
    )
}

// Graduation Mappings
fun GraduateTable.toLocal(): LocalGraduate {
    return LocalGraduate(
        items = this.items.map { it.toLocal() }
    )
}

fun GraduateTableCell.toLocal(): LocalGraduateCell {
    return LocalGraduateCell(
        classification = this.classification.orEmpty(),
        requirement = this.requirement.orEmpty(),
        standardValue = this.standardValue.orEmpty(),
        calculatedValue = this.calculatedValue.orEmpty(),
        difference = this.difference.orEmpty(),
        result = this.result.orEmpty()
    )
}

fun LocalGraduate.toDomain(): GraduateTable {
    return GraduateTable(
        items = this.items.map { it.toDomain() }
    )
}

fun LocalGraduateCell.toDomain(): GraduateTableCell {
    return GraduateTableCell(
        classification = this.classification,
        requirement = this.requirement,
        standardValue = this.standardValue,
        calculatedValue = this.calculatedValue,
        difference = this.difference,
        result = this.result
    )
}
