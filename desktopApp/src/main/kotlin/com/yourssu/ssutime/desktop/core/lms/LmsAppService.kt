package com.yourssu.ssutime.desktop.core.lms

import com.yourssu.data.DiscussionAttachment
import com.yourssu.data.DiscussionInfo
import com.yourssu.data.todoUniqueKey
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
private val TRAILING_COURSE_NUMBER = Regex("\\s*\\(\\d+\\)\\s*$")

class LmsAppService(
    private val api: SsuTimeApi,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun refreshTodos(
        loadingState: (Float) -> Unit = {},
        previousData: AppTodoData = AppTodoData(),
        onRequireReLogin: (suspend () -> Unit)? = null,
    ): LmsRefreshSnapshot {
        val terms = try {
            getLmsTerms()
        } catch (e: Exception) {
            if (onRequireReLogin != null) {
                onRequireReLogin()
                getLmsTerms()
            } else {
                throw e
            }
        }
        val currentTerm = terms.currentTermAt(Clock.System.now())
            ?: throw IllegalStateException("현재 진행 중인 학기 정보를 찾지 못했어요.")
        val subjects = try {
            getLmsTodoList(
                term = currentTerm,
                loadingState = loadingState,
            )
        } catch (e: Exception) {
            if (onRequireReLogin != null) {
                onRequireReLogin()
                getLmsTodoList(
                    term = currentTerm,
                    loadingState = loadingState,
                )
            } else {
                throw e
            }
        }

        val todoData = toAppTodoData(
            subjects = subjects,
            loadedAt = Clock.System.now().toString(),
            previousData = previousData,
        )

        return LmsRefreshSnapshot(
            todoData = todoData,
            subjects = todoData.subjects,
            semester = currentTerm.toString(),
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun loadTodosForTerm(
        term: Term,
        loadingState: (Float) -> Unit = {},
        previousData: AppTodoData = AppTodoData(),
        onRequireReLogin: (suspend () -> Unit)? = null,
    ): AppTodoData {
        val subjects = try {
            getLmsTodoList(
                term = term,
                loadingState = loadingState,
            )
        } catch (e: Exception) {
            if (onRequireReLogin != null) {
                onRequireReLogin()
                getLmsTodoList(
                    term = term,
                    loadingState = loadingState,
                )
            } else {
                throw e
            }
        }
        return toAppTodoData(
            subjects = subjects,
            loadedAt = Clock.System.now().toString(),
            previousData = previousData,
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun loadTerms(
        onRequireReLogin: (suspend () -> Unit)? = null,
    ): List<Term> {
        val terms = try {
            getLmsTerms()
        } catch (e: Exception) {
            if (onRequireReLogin != null) {
                onRequireReLogin()
                getLmsTerms()
            } else {
                throw e
            }
        }
        return terms.sortedWith(
            compareByDescending<Term> { it.start_at }
                .thenByDescending { it.id }
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun loadProfile(
        onRequireReLogin: (suspend () -> Unit)? = null,
    ): AppProfile {
        return try {
            val info = getLmsLoginInfo()
            val term = getLmsTerms().currentTermAt(Clock.System.now())
            AppProfile(
                name = info.user_name,
                department = info.dept_name,
                userId = info.user_login,
                email = info.user_email,
                termName = term?.name.orEmpty(),
            )
        } catch (e: Exception) {
            if (onRequireReLogin != null) {
                onRequireReLogin()
                val info = getLmsLoginInfo()
                val term = getLmsTerms().currentTermAt(Clock.System.now())
                AppProfile(
                    name = info.user_name,
                    department = info.dept_name,
                    userId = info.user_login,
                    email = info.user_email,
                    termName = term?.name.orEmpty(),
                )
            } else {
                throw e
            }
        }
    }

    suspend fun reportSnapshot(
        accessToken: String,
        snapshot: LmsRefreshSnapshot,
        onRefreshToken: (suspend () -> String?)? = null,
    ) {
        var currentToken = accessToken
        if (currentToken.isBlank()) {
            currentToken = onRefreshToken?.invoke().orEmpty()
            if (currentToken.isBlank()) return
        }

        var tokenRefreshed = false
        suspend fun refreshTokenIfNeeded(status: HttpStatusCode): Boolean {
            if (status.isAuthFailure() && !tokenRefreshed && onRefreshToken != null) {
                val nextToken = onRefreshToken()
                if (!nextToken.isNullOrBlank()) {
                    currentToken = nextToken
                    tokenRefreshed = true
                    return true
                }
            }
            return false
        }

        snapshot.subjects.forEach { subject ->
            runCatching {
                var status = api.addEnrollment(
                    accessToken = currentToken,
                    subject = subject,
                    semester = snapshot.semester,
                )
                if (refreshTokenIfNeeded(status)) {
                    status = api.addEnrollment(
                        accessToken = currentToken,
                        subject = subject,
                        semester = snapshot.semester,
                    )
                }
                check(status.isSuccess() || status == HttpStatusCode.Conflict) {
                    "수강 정보 등록 실패 (${status.value})"
                }
            }
        }

        (snapshot.todoData.todos + snapshot.todoData.submitted).forEach { todo ->
            runCatching {
                var status = api.reportTodo(currentToken, todo)
                if (refreshTokenIfNeeded(status)) {
                    status = api.reportTodo(currentToken, todo)
                }
                check(status.isSuccess()) {
                    "할 일 등록 실패 (${status.value})"
                }
            }
        }
    }

    suspend fun loadAiSummary(
        accessToken: String,
        todo: AppTodo,
        onRequireReLogin: (suspend () -> Unit)? = null,
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

        val session = try {
            getLmsCookies()
        } catch (e: Exception) {
            if (onRequireReLogin != null) {
                onRequireReLogin()
                getLmsCookies()
            } else {
                throw e
            }
        }
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

private fun String.withoutTrailingCourseNumber(): String =
    replace(TRAILING_COURSE_NUMBER, "")

private fun toAppTodoData(
    subjects: List<Subject>,
    loadedAt: String,
    previousData: AppTodoData = AppTodoData(),
): AppTodoData {
    val locallyReadDiscussionIds = previousData.subjects
        .flatMap { it.discussions }
        .filter { it.readState.equals("read", ignoreCase = true) }
        .map { it.id }
        .toSet()

    val subjectInfos = subjects.map { subject ->
        AppSubject(
            id = subject.id,
            name = subject.name.withoutTrailingCourseNumber(),
            professor = subject.professor,
            discussions = subject.discussions.map { discussion ->
                val isLocallyRead = discussion.id in locallyReadDiscussionIds
                val initialReadState = if (isLocallyRead) "read" else discussion.read_state.ifBlank { "unread" }
                DiscussionInfo(
                    id = discussion.id,
                    title = discussion.title,
                    message = discussion.message.orEmpty(),
                    url = discussion.url.orEmpty(),
                    published = discussion.published,
                    readState = initialReadState,
                    createdAt = discussion.created_at.orEmpty(),
                    author = discussion.user_name.orEmpty(),
                    attachments = discussion.attachments.map { att ->
                        DiscussionAttachment(
                            id = att.id,
                            name = att.display_name.ifBlank { att.file_name }.orEmpty(),
                            url = att.url.orEmpty(),
                        )
                    },
                )
            },
        )
    }.distinctBy { it.id }

    val subjectById = subjectInfos.associateBy { it.id }

    val allTodos = subjects.flatMap { subject ->
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
                url = todo.url.orEmpty(),
                duration = todo.durationOfVideo ?: -1.0,
                componentId = todo.component_id ?: -1,
                moduleItemId = todo.moduleItemId ?: -1,
            )
        }
    }.sortedWith(appTodoComparator())

    val submitted = subjects.flatMap { subject ->
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
                    url = submission.url.orEmpty(),
                )
            }
    }.sortedWith(appTodoComparator())

    val hiddenKeysSet = previousData.hiddenTodoKeys.toSet()
    val (hidden, active) = allTodos.partition { it.todoUniqueKey() in hiddenKeysSet }

    return previousData.copy(
        todos = active,
        submitted = submitted,
        subjects = subjectInfos,
        hiddenTodos = hidden,
        hiddenTodoKeys = previousData.hiddenTodoKeys,
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

private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299

private fun HttpStatusCode.isAuthFailure(): Boolean =
    this == HttpStatusCode.Unauthorized || this == HttpStatusCode.Forbidden

