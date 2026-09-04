package com.yourssu.ssutime.v2.screen.main

import android.util.Log
import com.yourssu.data.DiscussionAttachment
import com.yourssu.data.DiscussionInfo
import com.yourssu.data.LoginData
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.network.LmsSessionCookieRequest
import com.yourssu.data.network.LmsSessionRequest
import com.yourssu.data.network.toAddEnrollmentRequest
import com.yourssu.data.network.toTodoReportRequest
import com.yourssu.data.todoUniqueKey
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.lms.ensureLmsLoggedIn
import com.yourssu.ssutime.v2.lms.getLmsCookies
import com.yourssu.ssutime.v2.lms.getLmsTerms
import com.yourssu.ssutime.v2.lms.getLmsTodoList
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.todo.sortedByDeadlineThenName
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Subject
import io.github.chlwhdtn03.data.Lms.Submission
import io.github.chlwhdtn03.data.Lms.Term
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant as KotlinInstant

private const val BACKGROUND_REFRESH_RUNNING = "running"
private const val BACKGROUND_REFRESH_SUCCESS = "success"
private const val BACKGROUND_REFRESH_FAILED = "failed"
private const val LMS_API_TOKEN_ERROR_MESSAGE = "API 토큰값을 불러오지 못했습니다. 다시 시도해주세요."
private const val LMS_API_TOKEN_MAX_RETRIES = 2
private val BACKGROUND_REFRESH_LOCK_WINDOW: Duration = Duration.ofMinutes(10)
private val TRAILING_COURSE_NUMBER = Regex("\\s*\\(\\d+\\)\\s*$")

sealed interface LmsRefreshStage {
    data object LoggingIn : LmsRefreshStage
    data object LoadingTerms : LmsRefreshStage
    data class LoadingSubjects(val semester: String) : LmsRefreshStage
    data object SavingResult : LmsRefreshStage
}

class LmsRefreshRepository(
    private val loginRepository: LoginRepository,
    private val mainRepository: MainRepository,
    private val apiRepository: ApiRepository,
) {
    private val isRefreshing = AtomicBoolean(false)
    private val backendReportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _onboardingInitialRefreshInProgress = MutableStateFlow(false)
    private var onboardingInitialRefreshJob: Job? = null
    val onboardingInitialRefreshInProgress: StateFlow<Boolean> =
        _onboardingInitialRefreshInProgress.asStateFlow()

    suspend fun getLmsSessionRequest(): LmsSessionRequest {
        val loginData = loginRepository.getLoginData()
        loginIfNeeded(RefreshSource.FOREGROUND, loginData, forceLogin = false)
        val lmsSession = getLmsCookies()

        return LmsSessionRequest(
            cookies = lmsSession.cookies
                .filter { cookie ->
                    cookie.name.isNotBlank() &&
                        cookie.value.isNotBlank() &&
                        cookie.domain.isNotBlank() &&
                        cookie.path.isNotBlank()
                }
                .map { cookie ->
                    LmsSessionCookieRequest(
                        name = cookie.name,
                        value = cookie.value,
                        domain = cookie.domain,
                        path = cookie.path,
                    )
                },
        )
    }

    suspend fun refreshTodos(
        source: RefreshSource,
        requestId: String? = null,
        timeoutMillis: Long? = null,
        forceLogin: Boolean = false,
        loadingState: (Float) -> Unit = {},
        onRefreshStage: suspend (LmsRefreshStage) -> Unit = {},
    ): TodoRefreshResult {
        if (!isRefreshing.compareAndSet(false, true)) {
            return TodoRefreshResult.Skipped("이미 새로고침 중입니다.")
        }
        val startedAt = Instant.now()

        return try {
            val loginData = loginRepository.getLoginData()
            val currentData = mainRepository.getTodoData()

            if (source == RefreshSource.FCM) {
                val skipReason = getBackgroundSkipReason(
                    currentData = currentData,
                    loginData = loginData,
                    requestId = requestId,
                    now = startedAt,
                )
                if (skipReason != null) {
                    return TodoRefreshResult.Skipped(skipReason)
                }

                markBackgroundRefreshStarted(startedAt, requestId)
            }

            if (timeoutMillis != null) {
                withTimeout(timeoutMillis) {
                    executeRefresh(source, loginData, requestId, loadingState, forceLogin, onRefreshStage)
                }
            } else {
                executeRefresh(source, loginData, requestId, loadingState, forceLogin, onRefreshStage)
            }
        } catch (e: TimeoutCancellationException) {
            SentryExceptionReporter.capture(e)
            val message = "새로고침 시간이 초과됐어요."
            if (source == RefreshSource.FCM) {
                markBackgroundRefreshFailed(startedAt, requestId, message)
            }
            TodoRefreshResult.Failure(message, e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SentryExceptionReporter.capture(e)
            val message = e.message?.takeIf { it.isNotBlank() } ?: "LMS 정보를 불러오지 못했어요."
            if (source == RefreshSource.FCM) {
                markBackgroundRefreshFailed(startedAt, requestId, message)
            }
            TodoRefreshResult.Failure(message, e)
        } finally {
            isRefreshing.set(false)
        }
    }

    /**
     * Loads one term for temporary on-screen display without updating DataStore
     * or reporting the result to the backend.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun loadTodosForTerm(
        term: Term,
        forceLogin: Boolean = false,
        loadingState: (Float) -> Unit = {},
    ): TodoData = withContext(Dispatchers.IO) {
        val loginData = loginRepository.getLoginData()
        loginIfNeeded(
            source = RefreshSource.FOREGROUND,
            loginData = loginData,
            forceLogin = forceLogin,
        )
        val subjects = fetchSubjects(term, loginData, loadingState)
        buildTodoData(
            subjects = subjects,
            subjectInfos = buildSubjectInfos(subjects),
            previousData = mainRepository.getTodoData(),
            loadedAt = Instant.now().toString(),
        )
    }

    fun startOnboardingInitialRefresh() {
        if (onboardingInitialRefreshJob?.isActive == true) {
            return
        }

        onboardingInitialRefreshJob = appRefreshScope.launch {
            _onboardingInitialRefreshInProgress.value = true
            try {
                when (val result = refreshTodos(source = RefreshSource.ONBOARDING)) {
                    is TodoRefreshResult.Success -> {
                        Log.i(TAG, "온보딩 LMS 초기 새로고침이 완료되었습니다.")
                    }

                    is TodoRefreshResult.Failure -> {
                        Log.e(TAG, "온보딩 LMS 초기 새로고침에 실패했습니다: ${result.message}", result.throwable)
                    }

                    is TodoRefreshResult.Skipped -> {
                        Log.i(TAG, "온보딩 LMS 초기 새로고침을 건너뜁니다: ${result.reason}")
                    }
                }
            } finally {
                _onboardingInitialRefreshInProgress.value = false
            }
        }
    }

    private fun getBackgroundSkipReason(
        currentData: TodoData,
        loginData: LoginData,
        requestId: String?,
        now: Instant,
    ): String? {
        if (!loginData.hasAutoLoginCredentials) {
            Log.w(
                TAG,
                "백그라운드 새로고침 자동 로그인 정보 없음: " +
                    "isAutoLogin=${loginData.isAutoLogin}, " +
                    "hasId=${loginData.id.isNotBlank()}, " +
                    "hasPassword=${loginData.pw.isNotBlank()}",
            )
            return "자동 로그인 정보가 없어 백그라운드 새로고침을 건너뜁니다."
        }

        if (!requestId.isNullOrBlank() &&
            currentData.lastBackgroundRefreshRequestId == requestId &&
            currentData.lastBackgroundRefreshStatus == BACKGROUND_REFRESH_SUCCESS
        ) {
            return "이미 처리한 새로고침 요청입니다."
        }

        val lastStartedAt = currentData.lastBackgroundRefreshStartedAt.toInstantOrNull()
        val lastFinishedAt = currentData.lastBackgroundRefreshFinishedAt.toInstantOrNull()
        val lastRefreshStillRunning = lastStartedAt != null &&
            Duration.between(lastStartedAt, now) < BACKGROUND_REFRESH_LOCK_WINDOW &&
            (lastFinishedAt == null || lastFinishedAt.isBefore(lastStartedAt))

        return if (lastRefreshStillRunning) {
            "이미 백그라운드 새로고침 중입니다."
        } else {
            null
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun executeRefresh(
        source: RefreshSource,
        loginData: LoginData,
        requestId: String?,
        loadingState: (Float) -> Unit,
        forceLogin: Boolean,
        onRefreshStage: suspend (LmsRefreshStage) -> Unit,
    ): TodoRefreshResult.Success = withContext(Dispatchers.IO) {
        onRefreshStage(LmsRefreshStage.LoggingIn)
        loginIfNeeded(source, loginData, forceLogin)

        onRefreshStage(LmsRefreshStage.LoadingTerms)
        var terms: List<Term>
        var currentTerm: Term?
        try {
            terms = getLmsTerms()
            currentTerm = terms.currentTermAt(Clock.System.now())
        } catch (e: Exception) {
            if (loginData.hasAutoLoginCredentials) {
                Log.i(TAG, "LMS 세션 만료 의심으로 재로그인 후 재시도합니다.")
                loginIfNeeded(source, loginData, forceLogin = true)
                terms = getLmsTerms()
                currentTerm = terms.currentTermAt(Clock.System.now())
            } else {
                throw e
            }
        }
        if (currentTerm == null) {
            throw IllegalStateException("현재 진행 중인 학기 정보를 찾지 못했어요.")
        }
        onRefreshStage(LmsRefreshStage.LoadingSubjects(currentTerm.name.orEmpty()))
        val subjects = try {
            fetchSubjects(currentTerm, loginData, loadingState)
        } catch (e: Exception) {
            if (loginData.hasAutoLoginCredentials) {
                Log.i(TAG, "과목 조회 실패로 재로그인 후 재시도합니다.")
                loginIfNeeded(source, loginData, forceLogin = true)
                fetchSubjects(currentTerm, loginData, loadingState)
            } else {
                throw e
            }
        }
        val subjectInfos = buildSubjectInfos(subjects)
        val completedAt = Instant.now()
        val summary = buildRefreshSummary(
            completedAt = completedAt,
            terms = terms,
            subjects = subjects,
        )
        onRefreshStage(LmsRefreshStage.SavingResult)
        val todoData = saveRefreshResult(
            source = source,
            requestId = requestId,
            subjects = subjects,
            subjectInfos = subjectInfos,
            completedAt = completedAt,
            summary = summary,
        )

        reportRefreshResultToBackend(
            subjectInfos = subjectInfos,
            semester = currentTerm.toString(),
            todos = todoData.todos + todoData.submitted.filter { it.isReportableSubmission },
            loginData,
        )
        TodoRefreshResult.Success(todoData, summary)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun fetchSubjects(
        currentTerm: Term,
        loginData: LoginData,
        loadingState: (Float) -> Unit,
    ): List<Subject> = getLmsTodoList(
        term = currentTerm,
        loadingState = loadingState,
        postHogDistinctId = Analytics.currentPostHogDistinctId()
            ?: Analytics.postHogDistinctId(loginData.id),
    )

    @OptIn(ExperimentalTime::class)
    private fun buildRefreshSummary(
        completedAt: Instant,
        terms: List<Term>,
        subjects: List<Subject>,
    ): TodoRefreshSummary = TodoRefreshSummary(
        completedAt = completedAt,
        semesterCount = terms.size,
        subjectCount = subjects.size,
        todoCount = subjects.sumOf { it.todoList.size },
        submittedCount = subjects.sumOf { subject ->
            subject.submissions.count { it.submitted_at?.isNotEmpty() == true }
        },
    )

    private suspend fun saveRefreshResult(
        source: RefreshSource,
        requestId: String?,
        subjects: List<Subject>,
        subjectInfos: List<SubjectInfo>,
        completedAt: Instant,
        summary: TodoRefreshSummary,
    ): TodoData {
        val completedAtText = completedAt.toString()

        // 새로고침 중 갱신된 AI 캐시/알림 기록을 잃지 않도록 최신 DataStore 값에 병합한다.
        return mainRepository.updateTodoData { currentData ->
            val refreshedTodoData = buildTodoData(
                subjects = subjects,
                subjectInfos = subjectInfos,
                previousData = currentData,
                loadedAt = completedAtText,
            ).withWidgetRefreshCompleted(completedAtText)

            if (source == RefreshSource.FCM) {
                refreshedTodoData.withBackgroundRefreshSucceeded(completedAtText, requestId, summary)
            } else {
                refreshedTodoData
            }
        }
    }

    private fun TodoData.withWidgetRefreshCompleted(finishedAt: String): TodoData = copy(
        lastWidgetRefreshStatus = "",
        lastWidgetRefreshProgressMessage = "",
        lastWidgetRefreshErrorMessage = "",
        lastWidgetRefreshFinishedAt = finishedAt,
    )

    private fun TodoData.withBackgroundRefreshSucceeded(
        finishedAt: String,
        requestId: String?,
        summary: TodoRefreshSummary,
    ): TodoData = copy(
        lastBackgroundRefreshFinishedAt = finishedAt,
        lastBackgroundRefreshSuccessAt = finishedAt,
        lastBackgroundRefreshStatus = BACKGROUND_REFRESH_SUCCESS,
        lastBackgroundRefreshErrorMessage = "",
        lastBackgroundRefreshTodoCount = summary.todoCount,
        lastBackgroundRefreshSubmittedCount = summary.submittedCount,
        lastBackgroundRefreshSemesterCount = summary.semesterCount,
        lastBackgroundRefreshRequestId = requestId.orEmpty(),
    )

    private fun reportRefreshResultToBackend(
        subjectInfos: List<SubjectInfo>,
        semester: String,
        todos: List<TodoInfo>,
        loginData: LoginData,
    ) {
        backendReportScope.launch {
            reportEnrollmentsToBackend(subjectInfos, semester, loginData)
            reportTodosToBackend(todos, loginData)
        }
    }

    private suspend fun reportEnrollmentsToBackend(
        subjects: List<SubjectInfo>,
        semester: String,
        loginData: LoginData,
    ) {
        if (subjects.isEmpty() || !prepareBackendToken(loginData)) {
            return
        }

        var tokenRefreshAttempted = false
        subjects.forEach { subject ->
            runCatching {
                var status = apiRepository.addEnrollment(subject.toAddEnrollmentRequest(semester))
                if (status.isAuthFailure() && !tokenRefreshAttempted) {
                    tokenRefreshAttempted = true
                    if (refreshBackendToken(loginData)) {
                        status = apiRepository.addEnrollment(subject.toAddEnrollmentRequest(semester))
                    }
                }

                if (status.value !in 200..299 && status != HttpStatusCode.Conflict) {
                    Log.w(TAG, "Enrollment 백엔드 등록 실패: ${subject.name}, status=$status")
                }
            }.onFailure { exception ->
                SentryExceptionReporter.capture(exception)
                Log.e(TAG, "Enrollment 백엔드 등록 중 오류가 발생했습니다: ${subject.name}", exception)
            }
        }
    }

    private suspend fun reportTodosToBackend(todos: List<TodoInfo>, loginData: LoginData) {
        if (todos.isEmpty() || !prepareBackendToken(loginData)) {
            return
        }

        var tokenRefreshAttempted = false
        todos.forEach { todo ->
            runCatching {
                val request = todo.toTodoReportRequest()
                var status = apiRepository.reportTodo(request)
                if (status.isAuthFailure() && !tokenRefreshAttempted) {
                    tokenRefreshAttempted = true
                    if (refreshBackendToken(loginData)) {
                        status = apiRepository.reportTodo(request)
                    }
                }

                if (status.value !in 200..299) {
                    Log.w(TAG, "Todo 백엔드 제보 실패: ${todo.title}, status=$status")
                    Log.w(TAG, request.toString())
                }
            }.onFailure { exception ->
                SentryExceptionReporter.capture(exception)
                Log.e(TAG, "Todo 백엔드 제보 중 오류가 발생했습니다: ${todo.title}", exception)
            }
        }
    }

    private suspend fun prepareBackendToken(loginData: LoginData): Boolean {
        accessToken = accessToken.ifBlank { loginData.accessToken }
        if (accessToken.isNotBlank()) {
            return true
        }

        return refreshBackendToken(loginData)
    }

    private suspend fun refreshBackendToken(loginData: LoginData): Boolean {
        if (!loginData.hasAutoLoginCredentials) {
            Log.i(TAG, "백엔드 토큰이 없어 Todo 등록을 건너뜁니다.")
            return false
        }

        return runCatching {
            accessToken = apiRepository.requestJwtToken(loginData.id, loginData.pw).accessToken
            loginRepository.updateLoginData(loginData.copy(accessToken = accessToken))
        }.onFailure { exception ->
            SentryExceptionReporter.capture(exception)
            Log.e(TAG, "백엔드 토큰 갱신에 실패했습니다.", exception)
        }.isSuccess
    }

    private suspend fun loginIfNeeded(
        source: RefreshSource,
        loginData: LoginData,
        forceLogin: Boolean,
    ) {
        if (loginData.hasAutoLoginCredentials) {
            val force = forceLogin || source == RefreshSource.FCM
            val isLoggedIn = loginWithRetryIfNeeded(source, loginData, force = force)
            if (!isLoggedIn) {
                throw IllegalStateException("LMS 로그인에 실패했어요.")
            }
            return
        }

        if (!LmsApi.isLoggined) {
            throw IllegalStateException("LMS 로그인이 필요해요.")
        }
    }

    private suspend fun loginWithRetryIfNeeded(
        source: RefreshSource,
        loginData: LoginData,
        force: Boolean = false,
    ): Boolean {
        var attempt = 0

        while (true) {
            try {
                return ensureLmsLoggedIn(loginData.id, loginData.pw, force = force || attempt > 0)
            } catch (e: Exception) {
                val shouldRetry = source == RefreshSource.FCM &&
                    e.message == LMS_API_TOKEN_ERROR_MESSAGE &&
                    attempt < LMS_API_TOKEN_MAX_RETRIES
                if (!shouldRetry) {
                    throw e
                }
                attempt += 1
            }
        }
    }

    private fun buildTodoData(
        subjects: List<Subject>,
        subjectInfos: List<SubjectInfo>,
        previousData: TodoData,
        loadedAt: String,
    ): TodoData {
        val subjectInfoById = subjectInfos.associateBy { it.id }
        val newTodos = subjects.flatMap { subject ->
            subject.todoList.map { todo ->
                TodoInfo(
                    todoId = todo.assignment_id ?: -1,
                    title = todo.title,
                    due_date = todo.due_date,
                    type = TodoType.valueOf(todo.component_type.uppercase()),
                    subject = subjectInfoById[subject.id],
                    description = todo.description.orEmpty(),
                    url = todo.url.orEmpty(),
                    duration = todo.durationOfVideo ?: -1.0,
                    componentId = todo.component_id ?: -1,
                    moduleItemId = todo.moduleItemId ?: -1,
                )
            }
        }.sortedByDeadlineThenName()

        val reportedSubmitted = subjects.flatMap { subject ->
            Log.i(
                TAG,
                "LMS submissions titles: subject=${subject.name}, titles=${
                    subject.submissions.joinToString { it.name }
                }",
            )
            subject.submissions
                .filter { it.isReportableSubmission() }
                .map { todo ->
                    TodoInfo(
                        todoId = todo.assignment_id ?: -1,
                        title = todo.name,
                        due_date = todo.cached_due_date ?: "",
                        type = if (todo.late == true) TodoType.SUBMITTED_LATE else TodoType.SUBMITTED,
                        subject = subjectInfoById[subject.id],
                        submittedAt = todo.submitted_at.orEmpty(),
                        url = todo.url.orEmpty(),
                    )
                }
        }
        val newSubmitted = reportedSubmitted.sortedByDeadlineThenName()
        val locallyReadDiscussionIds = previousData.subjects
            .flatMap { it.discussions }
            .filter { it.readState.equals("read", ignoreCase = true) }
            .map { it.id }
            .toSet()

        val hiddenKeysSet = previousData.hiddenTodoKeys.toSet()
        val (hiddenNewTodos, activeNewTodos) = newTodos.partition { it.todoUniqueKey() in hiddenKeysSet }

        return previousData.copy(
            todos = activeNewTodos,
            submitted = newSubmitted,
            subjects = buildSubjectInfos(subjects, locallyReadDiscussionIds),
            hiddenTodos = hiddenNewTodos,
            loadedAt = loadedAt,
        )
    }

    private fun buildSubjectInfos(
        subjects: List<Subject>,
        locallyReadDiscussionIds: Set<Int> = emptySet(),
    ): List<SubjectInfo> = subjects
        .map { subject ->
            SubjectInfo(
                id = subject.id,
                name = subject.name.withoutTrailingCourseNumber(),
                professor = subject.professor,
                discussions = subject.discussions.map { discussion ->
                    val isLocallyRead = discussion.id in locallyReadDiscussionIds
                    val initialReadState = if (isLocallyRead) "read" else "unread"
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
                        }
                    )
                }
            )
        }
        .distinctBy { it.id }

    private suspend fun markBackgroundRefreshStarted(startedAt: Instant, requestId: String?) {
        mainRepository.updateTodoData { currentData ->
            currentData.copy(
                lastBackgroundRefreshStartedAt = startedAt.toString(),
                lastBackgroundRefreshFinishedAt = "",
                lastBackgroundRefreshStatus = BACKGROUND_REFRESH_RUNNING,
                lastBackgroundRefreshErrorMessage = "",
                lastBackgroundRefreshRequestId = requestId.orEmpty(),
            )
        }
    }

    private suspend fun markBackgroundRefreshFailed(
        startedAt: Instant,
        requestId: String?,
        message: String,
    ) {
        val finishedAt = Instant.now()
        mainRepository.updateTodoData { currentData ->
            currentData.copy(
                lastBackgroundRefreshStartedAt = currentData.lastBackgroundRefreshStartedAt.ifBlank {
                    startedAt.toString()
                },
                lastBackgroundRefreshFinishedAt = finishedAt.toString(),
                lastBackgroundRefreshStatus = BACKGROUND_REFRESH_FAILED,
                lastBackgroundRefreshErrorMessage = message,
                lastBackgroundRefreshRequestId = requestId.orEmpty(),
            )
        }
    }
}

internal fun String.withoutTrailingCourseNumber(): String =
    replace(TRAILING_COURSE_NUMBER, "")

enum class RefreshSource {
    FOREGROUND,
    APP_START,
    PULL_TO_REFRESH,
    REFRESH_BUTTON,
    WIDGET,
    ONBOARDING,
    FCM,
}

data class TodoRefreshSummary(
    val completedAt: Instant,
    val semesterCount: Int,
    val subjectCount: Int,
    val todoCount: Int,
    val submittedCount: Int,
)

sealed interface TodoRefreshResult {
    data class Success(
        val todoData: TodoData,
        val summary: TodoRefreshSummary,
    ) : TodoRefreshResult

    data class Failure(
        val message: String,
        val throwable: Throwable,
    ) : TodoRefreshResult

    data class Skipped(
        val reason: String,
    ) : TodoRefreshResult
}

private val TodoInfo.isReportableSubmission: Boolean
    get() = type == TodoType.SUBMITTED || type == TodoType.SUBMITTED_LATE

private fun HttpStatusCode.isAuthFailure(): Boolean =
    this == HttpStatusCode.Unauthorized || this == HttpStatusCode.Forbidden

@OptIn(ExperimentalTime::class)
internal fun List<Term>.currentTermAt(now: KotlinInstant): Term? =
    firstOrNull { term ->
        val startAt = term.start_at ?: return@firstOrNull false
        val endAt = term.end_at ?: return@firstOrNull false

        now in startAt..endAt
    }

private fun Submission.isReportableSubmission(): Boolean =
    submitted_at?.isNotBlank() == true ||
        workflow_state.equals("submitted", ignoreCase = true) ||
        workflow_state.equals("graded", ignoreCase = true) ||
        assignment_id == null ||
        assignment_id == -1

private const val TAG = "LmsRefreshRepository"

private fun String.toInstantOrNull(): Instant? = runCatching {
    Instant.parse(this)
}.getOrNull()
