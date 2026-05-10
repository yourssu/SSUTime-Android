package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.LoginData
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Subject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime

private const val BACKGROUND_REFRESH_RUNNING = "running"
private const val BACKGROUND_REFRESH_SUCCESS = "success"
private const val BACKGROUND_REFRESH_FAILED = "failed"
private const val LMS_API_TOKEN_ERROR_MESSAGE = "API 토큰값을 불러오지 못했습니다. 다시 시도해주세요."
private const val LMS_API_TOKEN_MAX_RETRIES = 2
private val BACKGROUND_REFRESH_LOCK_WINDOW: Duration = Duration.ofMinutes(10)

class LmsRefreshRepository(
    private val loginRepository: com.yourssu.ssutime.v2.screen.login.LoginRepository,
    private val mainRepository: MainRepository,
) {
    private val isRefreshing = AtomicBoolean(false)

    suspend fun refreshTodos(
        source: RefreshSource,
        requestId: String? = null,
        timeoutMillis: Long? = null,
        loadingState: (Float) -> Unit = {},
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
                    executeRefresh(source, loginData, requestId, loadingState)
                }
            } else {
                executeRefresh(source, loginData, requestId, loadingState)
            }
        } catch (e: TimeoutCancellationException) {
            val message = "새로고침 시간이 초과됐어요."
            if (source == RefreshSource.FCM) {
                markBackgroundRefreshFailed(startedAt, requestId, message)
            }
            TodoRefreshResult.Failure(message, e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = e.message?.takeIf { it.isNotBlank() } ?: "LMS 정보를 불러오지 못했어요."
            if (source == RefreshSource.FCM) {
                markBackgroundRefreshFailed(startedAt, requestId, message)
            }
            TodoRefreshResult.Failure(message, e)
        } finally {
            isRefreshing.set(false)
        }
    }

    private fun getBackgroundSkipReason(
        currentData: TodoData,
        loginData: LoginData,
        requestId: String?,
        now: Instant,
    ): String? {
        if (!loginData.hasAutoLoginCredentials) {
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
    ): TodoRefreshResult.Success = withContext(Dispatchers.IO) {
        loginIfNeeded(source, loginData)

        val terms = LmsApi.getTerms()
        val currentTerm = terms.firstOrNull()
            ?: throw IllegalStateException("학기 정보를 불러오지 못했어요.")
        val subjects = LmsApi.getTodoList(term = currentTerm, loadingState = loadingState)
        val completedAt = Instant.now()
        val previousData = mainRepository.getTodoData()
        val summary = TodoRefreshSummary(
            completedAt = completedAt,
            semesterCount = terms.size,
            subjectCount = subjects.size,
            todoCount = subjects.sumOf { it.todoList.size },
            submittedCount = subjects.sumOf { subject ->
                subject.submissions.count { it.submitted_at.isNotEmpty() }
            },
        )
        val refreshedTodoData = buildTodoData(
            subjects = subjects,
            previousData = previousData,
            loadedAt = completedAt.toString(),
        )
        val todoData = if (source == RefreshSource.FCM) {
            refreshedTodoData.copy(
                lastBackgroundRefreshFinishedAt = completedAt.toString(),
                lastBackgroundRefreshSuccessAt = completedAt.toString(),
                lastBackgroundRefreshStatus = BACKGROUND_REFRESH_SUCCESS,
                lastBackgroundRefreshErrorMessage = "",
                lastBackgroundRefreshTodoCount = summary.todoCount,
                lastBackgroundRefreshSubmittedCount = summary.submittedCount,
                lastBackgroundRefreshSemesterCount = summary.semesterCount,
                lastBackgroundRefreshRequestId = requestId.orEmpty(),
            )
        } else {
            refreshedTodoData
        }

        mainRepository.updateTodoData(todoData)
        TodoRefreshResult.Success(todoData, summary)
    }

    private suspend fun loginIfNeeded(source: RefreshSource, loginData: LoginData) {
        if (loginData.hasAutoLoginCredentials && (source == RefreshSource.FCM || !LmsApi.isLoggined)) {
            val isLoggedIn = loginWithRetryIfNeeded(source, loginData)
            if (!isLoggedIn) {
                throw IllegalStateException("LMS 로그인에 실패했어요.")
            }
            return
        }

        if (!LmsApi.isLoggined) {
            throw IllegalStateException("LMS 로그인이 필요해요.")
        }
    }

    private suspend fun loginWithRetryIfNeeded(source: RefreshSource, loginData: LoginData): Boolean {
        var attempt = 0

        while (true) {
            try {
                return LmsApi.loginLMS(loginData.id, loginData.pw)
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
        previousData: TodoData,
        loadedAt: String,
    ): TodoData {
        val subjectInfos = subjects
            .map { SubjectInfo(it.id, it.name, it.professor) }
            .distinctBy { it.id }
        val subjectInfoById = subjectInfos.associateBy { it.id }
        val newTodos = subjects.flatMap { subject ->
            subject.todoList.map { todo ->
                TodoInfo(
                    todo.assignment_id ?: -1,
                    todo.title,
                    todo.due_date,
                    TodoType.valueOf(todo.component_type.uppercase()),
                    subjectInfoById[subject.id],
                )
            }
        }.sortedBy { it.due_date }

        val newSubmitted = subjects.flatMap { subject ->
            subject.submissions
                .filter { it.submitted_at.isNotEmpty() }
                .map { todo ->
                    TodoInfo(
                        todo.assignment_id,
                        todo.name,
                        todo.cached_due_date,
                        if (todo.late) TodoType.SUBMITTED_LATE else TodoType.SUBMITTED,
                        subjectInfoById[subject.id],
                    )
                }
        }.sortedByDescending { it.due_date }

        return previousData.copy(
            todos = newTodos,
            submitted = newSubmitted,
            loadedAt = loadedAt,
        )
    }

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

enum class RefreshSource {
    MANUAL,
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

private val LoginData.hasAutoLoginCredentials: Boolean
    get() = isAutoLogin && id.isNotBlank() && pw.isNotBlank()

private fun String.toInstantOrNull(): Instant? = runCatching {
    Instant.parse(this)
}.getOrNull()
