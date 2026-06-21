package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.yourssu.data.LocalGrade
import com.yourssu.data.LocalGradeCell
import com.yourssu.data.LocalGradeSummaryCell
import com.yourssu.data.LocalGradeTable
import com.yourssu.data.LocalGraduate
import com.yourssu.data.LocalGraduateCell
import com.yourssu.data.LocalScholarship
import com.yourssu.data.LocalScholarshipCell
import com.yourssu.data.LocalTuition
import com.yourssu.data.LocalTuitionCell
import io.github.chlwhdtn03.data.Lms.GradeCell
import io.github.chlwhdtn03.data.Lms.GradeTable
import io.github.chlwhdtn03.data.Lms.GraduateTable
import io.github.chlwhdtn03.data.Lms.GraduateTableCell
import io.github.chlwhdtn03.data.Lms.ScholarshipHistoryCell
import io.github.chlwhdtn03.data.Lms.ScholarshipHistoryTable
import io.github.chlwhdtn03.data.Lms.SemesterGradeSummaryCell
import io.github.chlwhdtn03.data.Lms.SemesterGradeSummaryTable
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

val Context.gradeDataStore: DataStore<LocalGrade> by dataStore(
    fileName = "grade.json",
    serializer = GradeDataSerializer,
)

// Serializers
private val json = Json {
    ignoreUnknownKeys = true
}

object ScholarshipDataSerializer : Serializer<LocalScholarship> {
    override val defaultValue: LocalScholarship = LocalScholarship()
    override suspend fun readFrom(input: InputStream): LocalScholarship =
        try {
            json.decodeFromString<LocalScholarship>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            defaultValue
        }

    override suspend fun writeTo(t: LocalScholarship, output: OutputStream) {
        output.write(
            json.encodeToString(LocalScholarship.serializer(), t)
                .encodeToByteArray()
        )
    }
}

object TuitionDataSerializer : Serializer<LocalTuition> {
    override val defaultValue: LocalTuition = LocalTuition()
    override suspend fun readFrom(input: InputStream): LocalTuition =
        try {
            json.decodeFromString<LocalTuition>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            defaultValue
        }

    override suspend fun writeTo(t: LocalTuition, output: OutputStream) {
        output.write(
            json.encodeToString(LocalTuition.serializer(), t)
                .encodeToByteArray()
        )
    }
}

object GraduateDataSerializer : Serializer<LocalGraduate> {
    override val defaultValue: LocalGraduate = LocalGraduate()
    override suspend fun readFrom(input: InputStream): LocalGraduate =
        try {
            json.decodeFromString<LocalGraduate>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            defaultValue
        }

    override suspend fun writeTo(t: LocalGraduate, output: OutputStream) {
        output.write(
            json.encodeToString(LocalGraduate.serializer(), t)
                .encodeToByteArray()
        )
    }
}

object GradeDataSerializer : Serializer<LocalGrade> {
    override val defaultValue: LocalGrade = LocalGrade()
    override suspend fun readFrom(input: InputStream): LocalGrade =
        try {
            json.decodeFromString<LocalGrade>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            defaultValue
        }

    override suspend fun writeTo(t: LocalGrade, output: OutputStream) {
        output.write(
            json.encodeToString(LocalGrade.serializer(), t)
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

// Grade Mappings
fun SemesterGradeSummaryTable.toLocal(
    details: Map<String, LocalGradeTable>,
    thisSemesterYear: String?,
    thisSemesterType: String?
): LocalGrade {
    return LocalGrade(
        summaryItems = this.items.map { it.toLocal() },
        details = details,
        thisSemesterYear = thisSemesterYear,
        thisSemesterType = thisSemesterType
    )
}

fun SemesterGradeSummaryCell.toLocal(): LocalGradeSummaryCell {
    return LocalGradeSummaryCell(
        year = this.year.orEmpty(),
        semesterName = this.semester?.name.orEmpty(),
        semesterNameKor = this.semester?.nameKor.orEmpty(),
        gpa = this.gpa.orEmpty(),
        earnedCredits = this.earnedCredits.orEmpty(),
        semesterRank = this.semesterRank.orEmpty(),
        academicWarning = this.academicWarning.orEmpty(),
        attemptedCredits = this.attemptedCredits.orEmpty(),
        pfCredits = this.pfCredits.orEmpty(),
        gpaSum = this.gpaSum.orEmpty(),
        arithmeticMean = this.arithmeticMean.orEmpty(),
        totalRank = this.totalRank.orEmpty(),
        consultationStatus = this.consultationStatus.orEmpty(),
        failedYearStatus = this.failedYearStatus.orEmpty()
    )
}

fun GradeTable.toLocal(): LocalGradeTable {
    return LocalGradeTable(
        year = this.year.orEmpty(),
        semesterName = this.semester.name,
        semesterNameKor = this.semester.nameKor,
        items = this.items.map { it.toLocal() }
    )
}

fun GradeCell.toLocal(): LocalGradeCell {
    return LocalGradeCell(
        subjectName = this.subjectName.orEmpty(),
        professor = this.professor.orEmpty(),
        credits = this.credits.orEmpty(),
        gradePoint = this.gradePoint.orEmpty(),
        grade = this.grade.orEmpty(),
        subjectCode = this.subjectCode.orEmpty(),
        classification = this.classification.orEmpty()
    )
}

fun LocalGrade.toDomainSummary(): SemesterGradeSummaryTable {
    return SemesterGradeSummaryTable(
        items = this.summaryItems.map { it.toDomain() }
    )
}

fun LocalGradeSummaryCell.toDomain(): SemesterGradeSummaryCell {
    val sem = runCatching { io.github.chlwhdtn03.data.Lms.Semester.valueOf(this.semesterName) }.getOrDefault(io.github.chlwhdtn03.data.Lms.Semester.FIRST)
    return SemesterGradeSummaryCell(
        year = this.year,
        semester = sem,
        gpa = this.gpa,
        earnedCredits = this.earnedCredits,
        semesterRank = this.semesterRank,
        academicWarning = this.academicWarning,
        attemptedCredits = this.attemptedCredits,
        pfCredits = this.pfCredits,
        gpaSum = this.gpaSum,
        arithmeticMean = this.arithmeticMean,
        totalRank = this.totalRank,
        consultationStatus = this.consultationStatus,
        failedYearStatus = this.failedYearStatus
    )
}

fun LocalGradeTable.toDomain(): GradeTable {
    val sem = runCatching { io.github.chlwhdtn03.data.Lms.Semester.valueOf(this.semesterName) }.getOrDefault(io.github.chlwhdtn03.data.Lms.Semester.FIRST)
    return GradeTable(
        year = this.year,
        semester = sem,
        items = this.items.map { it.toDomain() }
    )
}

fun LocalGradeCell.toDomain(): GradeCell {
    return GradeCell(
        subjectName = this.subjectName,
        professor = this.professor,
        credits = this.credits,
        gradePoint = this.gradePoint,
        grade = this.grade,
        subjectCode = this.subjectCode,
        classification = this.classification
    )
}
