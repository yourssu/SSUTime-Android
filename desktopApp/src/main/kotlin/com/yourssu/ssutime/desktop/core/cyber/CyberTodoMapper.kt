package com.yourssu.ssutime.desktop.core.cyber

import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import io.github.chlwhdtn03.data.Cyber.CyberSubject
import io.github.chlwhdtn03.data.Cyber.CyberWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val CYBER_DEADLINE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

data class CyberTodoResult(
    val todos: List<TodoInfo> = emptyList(),
    val submitted: List<TodoInfo> = emptyList(),
    val subjects: List<SubjectInfo> = emptyList(),
)

object CyberTodoMapper {

    const val CYBER_LMS_URL = "https://lms.kcu.ac/atnlcSubj/atnlcApe/list"

    private val DATE_WITH_YEAR_REGEX = Regex("""(\d{4})[./-](\d{1,2})[./-](\d{1,2})""")
    private val DATE_MONTH_DAY_REGEX = Regex("""(\d{1,2})[./-](\d{1,2})""")
    private val TIME_REGEX = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?""")

    fun parseDeadlineIso(attendancePeriod: String, fallbackYear: Int = LocalDate.now(CYBER_DEADLINE_ZONE_ID).year): String {
        if (attendancePeriod.isBlank()) return ""

        val parts = attendancePeriod.split("~")
        val endPart = if (parts.size >= 2) parts[1].trim() else attendancePeriod.trim()

        val dateMatch = DATE_WITH_YEAR_REGEX.find(endPart)
        val (year, month, day) = if (dateMatch != null) {
            Triple(
                dateMatch.groupValues[1].toInt(),
                dateMatch.groupValues[2].toInt(),
                dateMatch.groupValues[3].toInt(),
            )
        } else {
            val mdMatch = DATE_MONTH_DAY_REGEX.find(endPart) ?: return ""
            Triple(
                fallbackYear,
                mdMatch.groupValues[1].toInt(),
                mdMatch.groupValues[2].toInt(),
            )
        }

        val timeMatch = TIME_REGEX.find(endPart)
        val (hour, minute, second) = if (timeMatch != null) {
            Triple(
                timeMatch.groupValues[1].toInt(),
                timeMatch.groupValues[2].toInt(),
                timeMatch.groupValues[3].toIntOrNull() ?: 0,
            )
        } else {
            Triple(23, 59, 59)
        }

        return runCatching {
            val localDate = LocalDate.of(year, month, day)
            val localTime = LocalTime.of(hour, minute, second)
            val zonedDateTime = ZonedDateTime.of(localDate, localTime, CYBER_DEADLINE_ZONE_ID)
            zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }.getOrDefault("")
    }

    fun toCyberSubjectId(subject: CyberSubject): Int {
        val key = "CYBER_${subject.courseCode}_${subject.deptCode}_${subject.year}_${subject.semesterCode}"
        val hash = key.hashCode().let { if (it == Int.MIN_VALUE) 1 else abs(it) }
        return -(100_000 + (hash % 1_000_000))
    }

    fun toCyberTodoId(subjectId: Int, weekNo: Int, lectureNo: Int): Int {
        val key = "$subjectId:$weekNo:$lectureNo"
        val hash = key.hashCode().let { if (it == Int.MIN_VALUE) 1 else abs(it) }
        return -(10_000 + (hash % 1_000_000))
    }

    fun parseDurationSeconds(timeStr: String): Double {
        if (timeStr.isBlank()) return -1.0
        val parts = timeStr.trim().split(":")
        return when (parts.size) {
            2 -> {
                val min = parts[0].toDoubleOrNull() ?: return -1.0
                val sec = parts[1].toDoubleOrNull() ?: return -1.0
                min * 60.0 + sec
            }
            3 -> {
                val hr = parts[0].toDoubleOrNull() ?: return -1.0
                val min = parts[1].toDoubleOrNull() ?: return -1.0
                val sec = parts[2].toDoubleOrNull() ?: return -1.0
                hr * 3600.0 + min * 60.0 + sec
            }
            else -> -1.0
        }
    }

    fun mapToSubjectInfo(subject: CyberSubject): SubjectInfo {
        return SubjectInfo(
            id = toCyberSubjectId(subject),
            name = subject.name,
            professor = subject.professor,
            discussions = emptyList(),
        )
    }

    fun mapWeeksToTodos(
        subject: CyberSubject,
        subjectInfo: SubjectInfo,
        weeks: List<CyberWeek>,
    ): Pair<List<TodoInfo>, List<TodoInfo>> {
        val todos = mutableListOf<TodoInfo>()
        val submitted = mutableListOf<TodoInfo>()

        for (week in weeks) {
            val dueDate = parseDeadlineIso(week.attendancePeriod)
            val isAttendanceDone = week.attendanceStatus.contains("출석") && !week.attendanceStatus.contains("미출석")
            val isAttendanceLate = week.attendanceStatus.contains("지각")
            val isAttendanceAbsent = week.attendanceStatus.contains("결석")

            if (week.lectures.isEmpty()) {
                val isCompleted = isAttendanceDone || isAttendanceLate || isAttendanceAbsent
                val todoType = when {
                    isAttendanceLate -> TodoType.SUBMITTED_LATE
                    isCompleted -> TodoType.SUBMITTED
                    else -> TodoType.COMMONS
                }
                val title = buildString {
                    append("${week.weekNo}주차")
                    if (week.topic.isNotBlank()) {
                        append(" - ${week.topic}")
                    }
                }
                val todoInfo = TodoInfo(
                    todoId = toCyberTodoId(subjectInfo.id, week.weekNo, 0),
                    title = title,
                    due_date = dueDate,
                    type = todoType,
                    subject = subjectInfo,
                    url = CYBER_LMS_URL,
                )
                if (isCompleted) {
                    submitted.add(todoInfo)
                } else {
                    todos.add(todoInfo)
                }
            } else {
                for (lecture in week.lectures) {
                    val isCompleted = lecture.isCompleted || isAttendanceDone || isAttendanceLate || isAttendanceAbsent
                    val todoType = when {
                        isAttendanceLate -> TodoType.SUBMITTED_LATE
                        isCompleted -> TodoType.SUBMITTED
                        else -> TodoType.COMMONS
                    }
                    val title = buildString {
                        append("${week.weekNo}주차 ${lecture.lectureNo}강")
                        if (week.topic.isNotBlank()) {
                            append(" - ${week.topic}")
                        }
                    }
                    val duration = parseDurationSeconds(lecture.baseTime.ifBlank { lecture.studyTime })
                    val todoInfo = TodoInfo(
                        todoId = toCyberTodoId(subjectInfo.id, week.weekNo, lecture.lectureNo),
                        title = title,
                        due_date = dueDate,
                        type = todoType,
                        subject = subjectInfo,
                        duration = duration,
                        url = CYBER_LMS_URL,
                    )
                    if (isCompleted) {
                        submitted.add(todoInfo)
                    } else {
                        todos.add(todoInfo)
                    }
                }
            }
        }

        return Pair(todos, submitted)
    }
}
