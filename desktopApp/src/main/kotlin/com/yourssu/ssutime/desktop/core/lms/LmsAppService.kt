package com.yourssu.ssutime.desktop.core.lms

import com.yourssu.ssutime.desktop.core.model.AiSummary
import com.yourssu.ssutime.desktop.core.model.AppProfile
import com.yourssu.ssutime.desktop.core.model.AppSubject
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import com.yourssu.ssutime.desktop.core.model.canRequestAiSummary
import com.yourssu.ssutime.desktop.core.model.dueDate
import com.yourssu.ssutime.desktop.core.network.LmsCookie
import com.yourssu.ssutime.desktop.core.network.SsuTimeApi
import com.yourssu.ssutime.desktop.core.network.toConfirmedAiSummaryOrNull
import com.yourssu.ssutime.desktop.lms.getLmsCookies
import com.yourssu.ssutime.desktop.lms.getLmsLoginInfo
import com.yourssu.ssutime.desktop.lms.getLmsTerms
import com.yourssu.ssutime.desktop.lms.getLmsTodoList
import io.github.chlwhdtn03.data.Lms.Subject
import io.github.chlwhdtn03.data.Lms.Submission
import io.github.chlwhdtn03.data.Lms.Term
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val AI_SUMMARY_POLL_ATTEMPTS = 8
private const val AI_SUMMARY_POLL_INTERVAL_MILLIS = 2_000L

class LmsAppService(
    private val api: SsuTimeApi,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun refreshTodos(
        loadingState: (Float) -> Unit = {},
    ): LmsRefreshSnapshot {
        val terms = getLmsTerms()
        val currentTerm = terms.currentTermAt(Clock.System.now())
            ?: throw IllegalStateException("현재 진행 중인 학기 정보를 찾지 못했어요.")
        val subjects = getLmsTodoList(
            term = currentTerm,
            loadingState = loadingState,
        )

        return LmsRefreshSnapshot(
            todoData = subjects.toAppTodoData(Clock.System.now().toString()),
            subjects = subjects
                .map { subject ->
                    AppSubject(
                        id = subject.id,
                        name = subject.name,
                        professor = subject.professor,
                    )
                }
                .distinctBy(AppSubject::id),
            semester = currentTerm.toString(),
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun loadProfile(): AppProfile {
        val info = getLmsLoginInfo()
        val term = getLmsTerms().currentTermAt(Clock.System.now())
        return AppProfile(
            name = info.user_name,
            department = info.dept_name,
            userId = info.user_login,
            email = info.user_email,
            termName = term?.name.orEmpty(),
        )
    }

    suspend fun reportSnapshot(
        accessToken: String,
        snapshot: LmsRefreshSnapshot,
    ) {
        if (accessToken.isBlank()) return

        snapshot.subjects.forEach { subject ->
            runCatching {
                val status = api.addEnrollment(
                    accessToken = accessToken,
                    subject = subject,
                    semester = snapshot.semester,
                )
                check(status.isSuccess() || status == HttpStatusCode.Conflict) {
                    "수강 정보 등록 실패 (${status.value})"
                }
            }
        }

        (snapshot.todoData.todos + snapshot.todoData.submitted).forEach { todo ->
            runCatching {
                val status = api.reportTodo(accessToken, todo)
                check(status.isSuccess()) {
                    "할 일 등록 실패 (${status.value})"
                }
            }
        }
    }

    suspend fun loadAiSummary(
        accessToken: String,
        todo: AppTodo,
    ): AiSummary? {
        if (!todo.canRequestAiSummary || accessToken.isBlank()) {
            return null
        }

        val cached = runCatching {
            api.findAiSummary(accessToken, todo)?.toConfirmedAiSummaryOrNull()
        }.getOrNull()
        if (cached != null) {
            return cached
        }

        val session = getLmsCookies()
        val cookies = session.cookies
            .filter { cookie ->
                cookie.name.isNotBlank() &&
                    cookie.value.isNotBlank() &&
                    cookie.domain.isNotBlank() &&
                    cookie.path.isNotBlank()
            }
            .map { cookie ->
                LmsCookie(
                    name = cookie.name,
                    value = cookie.value,
                    domain = cookie.domain,
                    path = cookie.path,
                )
            }

        api.reportTodoWithAnalysis(
            accessToken = accessToken,
            todo = todo,
            cookies = cookies,
        )

        repeat(AI_SUMMARY_POLL_ATTEMPTS) { attempt ->
            val summary = try {
                api.findAiSummary(accessToken, todo)?.toConfirmedAiSummaryOrNull()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            if (summary != null) {
                return summary
            }
            if (attempt < AI_SUMMARY_POLL_ATTEMPTS - 1) {
                delay(AI_SUMMARY_POLL_INTERVAL_MILLIS)
            }
        }
        return null
    }
}

data class LmsRefreshSnapshot(
    val todoData: AppTodoData,
    val subjects: List<AppSubject>,
    val semester: String,
)

@OptIn(ExperimentalTime::class)
fun List<Term>.currentTermAt(now: Instant): Term? =
    firstOrNull { term ->
        val startAt = term.start_at ?: return@firstOrNull false
        val endAt = term.end_at ?: return@firstOrNull false
        now in startAt..endAt
    }

private fun List<Subject>.toAppTodoData(loadedAt: String): AppTodoData {
    val subjectById = associate { subject ->
        subject.id to AppSubject(
            id = subject.id,
            name = subject.name,
            professor = subject.professor,
        )
    }
    val todos = flatMap { subject ->
        subject.todoList.mapNotNull { todo ->
            val type = runCatching {
                AppTodoType.valueOf(todo.component_type.uppercase())
            }.getOrNull() ?: return@mapNotNull null
            AppTodo(
                todoId = todo.assignment_id ?: -1,
                title = todo.title,
                due_date = todo.due_date,
                type = type,
                subject = subjectById[subject.id],
                description = todo.description.orEmpty(),
            )
        }
    }.sortedWith(appTodoComparator())

    val submitted = flatMap { subject ->
        subject.submissions
            .filter(Submission::isReportableSubmission)
            .map { submission ->
                AppTodo(
                    todoId = submission.assignment_id ?: -1,
                    title = submission.name,
                    due_date = submission.cached_due_date.orEmpty(),
                    type = if (submission.late == true) {
                        AppTodoType.SUBMITTED_LATE
                    } else {
                        AppTodoType.SUBMITTED
                    },
                    subject = subjectById[subject.id],
                    submittedAt = submission.submitted_at.orEmpty(),
                )
            }
    }.sortedWith(appTodoComparator())

    return AppTodoData(
        todos = todos,
        submitted = submitted.filterRecentlySubmitted(),
        loadedAt = loadedAt,
    )
}

private fun appTodoComparator(): Comparator<AppTodo> =
    compareBy<AppTodo> { todo -> todo.dueDate }
        .thenBy { todo -> todo.subject?.name.orEmpty() }
        .thenBy(AppTodo::title)
        .thenBy(AppTodo::todoId)

private fun Submission.isReportableSubmission(): Boolean =
    submitted_at?.isNotBlank() == true ||
        workflow_state.equals("submitted", ignoreCase = true) ||
        workflow_state.equals("graded", ignoreCase = true) ||
        assignment_id == null ||
        assignment_id == -1

@OptIn(ExperimentalTime::class)
private fun List<AppTodo>.filterRecentlySubmitted(
    now: Instant = Clock.System.now(),
): List<AppTodo> = filter { todo ->
    val submittedAt = runCatching { Instant.parse(todo.submittedAt) }.getOrNull()
        ?: return@filter false
    now - submittedAt <= kotlin.time.Duration.parse("24h")
}

private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299
