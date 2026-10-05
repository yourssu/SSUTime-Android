package com.yourssu.ssutime.desktop.core.lms

import com.yourssu.data.AttachmentInfo
import com.yourssu.data.network.isFailed
import com.yourssu.data.network.isSkipped
import com.yourssu.data.network.hasNoAnalyzableAttachment
import com.yourssu.ssutime.desktop.core.network.DesktopApiException
import com.yourssu.data.NewTodoNotificationRecord
import com.yourssu.data.DiscussionAttachment
import com.yourssu.data.DiscussionInfo
import com.yourssu.data.isCyber
import com.yourssu.data.todoUniqueKey
import com.yourssu.ssutime.desktop.ui.util.toTodoDeadlineInstantOrNull
import com.yourssu.ssutime.desktop.ui.util.sortedByDeadlineThenName
import com.yourssu.ssutime.desktop.ui.util.withSubmissionOrder
import com.yourssu.ssutime.desktop.core.cyber.CyberTodoResult
import com.yourssu.ssutime.desktop.core.model.AiSummary
import com.yourssu.ssutime.desktop.core.model.AppProfile
import com.yourssu.ssutime.desktop.core.model.AppSubject
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import com.yourssu.ssutime.desktop.core.model.canRequestAiSummary
import com.yourssu.ssutime.desktop.core.model.dueDate
import com.yourssu.ssutime.desktop.core.model.submittedTodoComparator
import com.yourssu.ssutime.desktop.core.network.LmsCookie
import com.yourssu.ssutime.desktop.core.network.SsuTimeApi
import com.yourssu.ssutime.desktop.core.network.toAiSummaryOrNull
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
        cyberResult: CyberTodoResult = CyberTodoResult(),
        isCyberConnected: Boolean = false,
        onRequireReLogin: (suspend () -> Unit)? = null,
        postHogDistinctId: String? = null,
    ): LmsRefreshSnapshot {
        val terms = try {
            getLmsTerms()
        } catch (cancellation: CancellationException) {
            throw cancellation
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
                postHogDistinctId = postHogDistinctId,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            if (onRequireReLogin != null) {
                onRequireReLogin()
                getLmsTodoList(
                    term = currentTerm,
                    loadingState = loadingState,
                    postHogDistinctId = postHogDistinctId,
                )
            } else {
                throw e
            }
        }

        val todoData = toAppTodoData(
            subjects = subjects,
            loadedAt = Clock.System.now().toString(),
            previousData = previousData,
            cyberResult = cyberResult,
            isCyberConnected = isCyberConnected,
        )

        val knownKeys = (previousData.todos + previousData.hiddenTodos + previousData.submitted)
            .map { it.todoUniqueKey() }.toSet()
        val discovered = if (previousData.loadedAt.isBlank()) emptyList() else todoData.todos
            .filter { it.type in listOf(AppTodoType.ASSIGNMENT, AppTodoType.QUIZ, AppTodoType.COMMONS) && it.todoUniqueKey() !in knownKeys }
            .map { NewTodoNotificationRecord(it.todoUniqueKey(), todoData.loadedAt, it.subject?.name.orEmpty(), it.type) }
        return LmsRefreshSnapshot(
            todoData = todoData.copy(pendingNewTodoNotifications = (previousData.pendingNewTodoNotifications + discovered).distinctBy { it.todoKey }),
            subjects = todoData.subjects,
            semester = currentTerm.toString(),
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun loadTodosForTerm(
        term: Term,
        loadingState: (Float) -> Unit = {},
        previousData: AppTodoData = AppTodoData(),
        cyberResult: CyberTodoResult = CyberTodoResult(),
        isCyberConnected: Boolean = false,
        onRequireReLogin: (suspend () -> Unit)? = null,
        postHogDistinctId: String? = null,
    ): AppTodoData {
        val subjects = try {
            getLmsTodoList(
                term = term,
                loadingState = loadingState,
                postHogDistinctId = postHogDistinctId,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            if (onRequireReLogin != null) {
                onRequireReLogin()
                getLmsTodoList(
                    term = term,
                    loadingState = loadingState,
                    postHogDistinctId = postHogDistinctId,
                )
            } else {
                throw e
            }
        }
        return toAppTodoData(
            subjects = subjects,
            loadedAt = Clock.System.now().toString(),
            previousData = previousData,
            cyberResult = cyberResult,
            isCyberConnected = isCyberConnected,
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun loadTerms(
        onRequireReLogin: (suspend () -> Unit)? = null,
    ): List<Term> {
        val terms = try {
            getLmsTerms()
        } catch (cancellation: CancellationException) {
            throw cancellation
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
        } catch (cancellation: CancellationException) {
            throw cancellation
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

        snapshot.subjects.filter { it.id > 0 }.forEach { subject ->
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

        (snapshot.todoData.todos + snapshot.todoData.submitted).filterNot { it.isCyber() }.forEach { todo ->
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
        onRefreshToken: (suspend () -> String?)? = null,
    ): AiSummary? {
        if (!todo.canRequestAiSummary || accessToken.isBlank()) {
            return null
        }

        var token = accessToken
        suspend fun <T> authenticated(block: suspend (String) -> T): T {
            return try { block(token) } catch (error: DesktopApiException) {
                if (error.status.value !in listOf(401, 403) || onRefreshToken == null) throw error
                token = onRefreshToken().orEmpty()
                if (token.isBlank()) throw error
                block(token)
            }
        }
        val cached = runCatching {
            authenticated { api.findAiSummary(it, todo) }?.toAiSummaryOrNull()
        }.getOrNull()
        if (cached != null) {
            return cached
        }

        val session = try {
            getLmsCookies()
        } catch (cancellation: CancellationException) {
            throw cancellation
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

        val analysis = authenticated { api.reportTodoWithAnalysis(it, todo, cookies) }
        if (analysis?.isFailed == true || analysis?.isSkipped == true || analysis?.hasNoAnalyzableAttachment == true) {
            return authenticated { api.findAiSummary(it, todo) }?.toAiSummaryOrNull()
        }

        repeat(AI_SUMMARY_POLL_ATTEMPTS) { attempt ->
            val summary = try {
                authenticated { api.findAiSummary(it, todo) }?.toAiSummaryOrNull()
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

internal fun toAppTodoData(
    subjects: List<Subject>,
    loadedAt: String,
    previousData: AppTodoData = AppTodoData(),
    cyberResult: CyberTodoResult = CyberTodoResult(),
    isCyberConnected: Boolean = false,
    now: java.time.Instant = java.time.Instant.now(),
): AppTodoData {
    val locallyReadDiscussionIds = (previousData.readDiscussionIds + previousData.subjects
        .flatMap { it.discussions }
        .filter { it.readState.equals("read", ignoreCase = true) }
        .map { it.id }).toSet()

    val lmsSubjectInfos = subjects.map { subject ->
        AppSubject(
            id = subject.id,
            name = subject.name.withoutTrailingCourseNumber(),
            professor = subject.professor,
            discussions = subject.discussions.map { discussion ->
                val isLocallyRead = discussion.id in locallyReadDiscussionIds
                val initialReadState = if (isLocallyRead || discussion.read_state.equals("read", true)) "read" else "unread"
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

    val cyberSubjects = if (!isCyberConnected) emptyList() else if (!cyberResult.isComplete && cyberResult.subjects.isEmpty()) {
        previousData.subjects.filter { it.id < 0 }
    } else cyberResult.subjects
    val allSubjectInfos = (lmsSubjectInfos + cyberSubjects).distinctBy { it.id }
    val subjectById = allSubjectInfos.associateBy { it.id }

    val lmsTodos = subjects.flatMap { subject ->
        subject.todoList.mapNotNull { todo ->
            val type = runCatching {
                AppTodoType.valueOf(todo.component_type.uppercase())
            }.getOrNull() ?: return@mapNotNull null
            AppTodo(
                todoId = todo.assignment_id ?: -1,
                title = todo.title,
                due_date = todo.due_date,
                lateAt = todo.late_at.orEmpty(),
                type = type,
                subject = subjectById[subject.id],
                description = todo.description.orEmpty(),
                url = todo.url.orEmpty(),
                duration = todo.durationOfVideo ?: -1.0,
                componentId = todo.component_id ?: -1,
                moduleItemId = todo.moduleItemId ?: -1,
                attachments = todo.attachments?.map {
                    AttachmentInfo(
                        id = it.id,
                        uuid = it.uuid,
                        folder_id = it.folder_id,
                        display_name = it.display_name,
                        file_name = it.file_name,
                        content_type = it.content_type,
                        size = it.size,
                        url = it.url,
                        thumbnail_url = it.thumbnail_url,
                        created_at = it.created_at,
                        updated_at = it.modified_at,
                        modified_at = it.modified_at,
                        mime_class = it.mime_class,
                    )
                } ?: emptyList(),
            )
        }
    }

    val lmsSubmitted = subjects.flatMap { subject ->
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
                    attachments = submission.attachments?.map {
                        AttachmentInfo(
                            id = it.id,
                            uuid = it.uuid,
                            folder_id = it.folder_id,
                            display_name = it.display_name,
                            file_name = it.file_name,
                            content_type = it.content_type,
                            size = it.size,
                            url = it.url,
                            thumbnail_url = it.thumbnail_url,
                            created_at = it.created_at,
                            updated_at = it.updated_at,
                            modified_at = it.modified_at,
                            mime_class = it.mime_class,
                        )
                    } ?: emptyList(),
                )
            }
    }

    val allTodos = (lmsTodos + if (isCyberConnected) cyberResult.todos else emptyList()).sortedByDeadlineThenName()
    val allSubmitted = (lmsSubmitted + if (isCyberConnected) cyberResult.submitted else emptyList())
        .sortedWith(submittedTodoComparator())
    val currentSubjectIds = allSubjectInfos.map { it.id }.toSet()
    fun canPreserve(todo: AppTodo): Boolean =
        (todo.subject?.id ?: todo.subjectId) in currentSubjectIds &&
            !(todo.isCyber() && (!isCyberConnected || cyberResult.isComplete))
    val preservedTodos = (previousData.todos + previousData.hiddenTodos).filter { previous ->
        canPreserve(previous) &&
            allTodos.none { isSameTodoItem(it, previous) } &&
            allSubmitted.none { isSameTodoItem(it, previous) } &&
            previous.dueDate.toTodoDeadlineInstantOrNull()?.isBefore(now) != true
    }.map { it.copy(subject = subjectById[it.subject?.id ?: it.subjectId] ?: it.subject) }
    val mergedTodos = (allTodos + preservedTodos).distinctBy { it.todoUniqueKey() }.sortedByDeadlineThenName()
    val preservedSubmitted = previousData.submitted.filter { previous ->
        canPreserve(previous) && allSubmitted.none { isSameTodoItem(it, previous) } &&
            mergedTodos.none { isSameTodoItem(it, previous) }
    }.map { it.copy(subject = subjectById[it.subject?.id ?: it.subjectId] ?: it.subject) }
    val mergedSubmitted = (allSubmitted + preservedSubmitted).distinctBy { it.todoUniqueKey() }
        .sortedWith(submittedTodoComparator())
    val (hidden, active) = mergedTodos.partition { it.todoUniqueKey() in previousData.hiddenTodoKeys }
    return previousData.copy(
        todos = active,
        submitted = mergedSubmitted,
        subjects = allSubjectInfos,
        hiddenTodos = hidden,
        loadedAt = loadedAt,
        readDiscussionIds = (locallyReadDiscussionIds + allSubjectInfos.flatMap { it.discussions }
            .filter { it.readState.equals("read", true) }.map { it.id }).toList(),
    ).withSubmissionOrder()
}

internal fun isSameTodoItem(a: AppTodo, b: AppTodo): Boolean {
    val aSubjectId = a.subject?.id ?: a.subjectId
    val bSubjectId = b.subject?.id ?: b.subjectId
    if (aSubjectId != bSubjectId && aSubjectId > 0 && bSubjectId > 0) return false
    if (a.todoId > 0 && b.todoId > 0) return a.todoId == b.todoId
    if (a.isCyber() && b.isCyber() && a.todoId != 0 && b.todoId != 0) return a.todoId == b.todoId
    if (a.moduleItemId > 0 && b.moduleItemId > 0) return a.moduleItemId == b.moduleItemId
    if (a.componentId > 0 && b.componentId > 0) return a.componentId == b.componentId
    return a.title.trim().isNotEmpty() && a.title.trim().equals(b.title.trim(), true) &&
        (a.dueDate.isBlank() || b.dueDate.isBlank() || a.dueDate == b.dueDate)
}

private fun Submission.isReportableSubmission(): Boolean =
    submitted_at?.isNotBlank() == true ||
        workflow_state.equals("submitted", ignoreCase = true) ||
        workflow_state.equals("graded", ignoreCase = true) ||
        assignment_id == null ||
        assignment_id == -1

private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299

private fun HttpStatusCode.isAuthFailure(): Boolean =
    this == HttpStatusCode.Unauthorized || this == HttpStatusCode.Forbidden

