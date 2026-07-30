package com.yourssu.ssutime.v2.screen.main

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AiSummaryCache
import com.yourssu.data.AlertData
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.network.ReportedTodoResponse
import com.yourssu.data.network.matches
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.ExperimentalTime

private val REFRESH_DATE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
private val SUBMITTED_VISIBLE_WINDOW: Duration = Duration.ofHours(24)
private const val AI_SUMMARY_POLL_ATTEMPTS = 8
private const val AI_SUMMARY_POLL_INTERVAL_MILLIS = 2_000L
private const val TODO_STATUS_PROVISIONAL = "PROVISIONAL"
private const val TODO_STATUS_CONFIRMED = "CONFIRMED"

@OptIn(ExperimentalTime::class)
class MainViewModel(
    private val mainRepository: MainRepository,
    private val lmsRefreshRepository: LmsRefreshRepository,
    private val termSelectionStore: TermSelectionStore,
) : ViewModel() {
    var todos = mutableStateListOf<TodoInfo>()
    var submitted = mutableStateListOf<TodoInfo>()
    var isLoading = mutableStateOf(false)
    var showLoading = mutableStateOf(false)
    var loadingProgress = mutableFloatStateOf(0f)
    var loadedAt = mutableStateOf("")
    var showNetworkError = mutableStateOf(false)
    var showNetworkCause = mutableStateOf("")
    var showWidgetBadge = mutableStateOf(false)
    var onboardingInitialRefreshInProgress = mutableStateOf(false)
    val aiSummaryStates = mutableStateMapOf<String, AiSummaryUiState>()

    var requiredShowAlertBottomSheet = mutableStateOf(false)
    private var handledHomeEntryVersion: Int? = null

    init {
        viewModelScope.launch {
            val alertData = mainRepository.getAlertData()
            requiredShowAlertBottomSheet.value = !alertData.valid
            showWidgetBadge.value = alertData.showWidgetHelperBadge
        }

        viewModelScope.launch {
            mainRepository.todoData.collect { todoData ->
                if (termSelectionStore.selectedTerm.value == null) {
                    updateTodoState(todoData)
                }
            }
        }

        viewModelScope.launch {
            termSelectionStore.selectedTerm.collectLatest { selectedTerm ->
                if (selectedTerm == null) {
                    showNetworkError.value = false
                    aiSummaryStates.clear()
                    updateTodoState(mainRepository.getTodoData())
                } else {
                    loadSelectedTermTodos(selectedTerm)
                }
            }
        }

        viewModelScope.launch {
            lmsRefreshRepository.onboardingInitialRefreshInProgress.collect { isRefreshing ->
                onboardingInitialRefreshInProgress.value = isRefreshing
            }
        }
    }

    fun shouldRunInitialLoad(homeEntryVersion: Int): Boolean {
        if (handledHomeEntryVersion == homeEntryVersion) {
            return false
        }

        handledHomeEntryVersion = homeEntryVersion
        return true
    }

    fun updateAlertState(alertData: AlertData) {
        viewModelScope.launch {
            mainRepository.updateAlertData(alertData)
        }
        requiredShowAlertBottomSheet.value = false
    }

    fun showNetworkErrorScreen() {
        showNetworkError.value = true
    }

    fun dismissWidgetHelperBadge() {
        showWidgetBadge.value = false
        viewModelScope.launch {
            mainRepository.dismissWidgetHelperBadge()
        }
    }

    fun loadAiSummary(todo: TodoInfo) {
        if (!todo.canRequestAiSummary()) {
            return
        }

        val key = todo.aiSummaryKey()
        when (aiSummaryStates[key]) {
            AiSummaryUiState.Loading,
            AiSummaryUiState.Analyzing -> return
            is AiSummaryUiState.Success,
            AiSummaryUiState.Empty,
            AiSummaryUiState.Error,
            null -> Unit
        }

        viewModelScope.launch {
            val fallbackSuccess = (aiSummaryStates[key] as? AiSummaryUiState.Success)
                ?: mainRepository.getCachedAiSummary(key)?.toAiSummarySuccessOrNull()

            if (fallbackSuccess != null) {
                aiSummaryStates[key] = fallbackSuccess
            } else {
                aiSummaryStates[key] = AiSummaryUiState.Loading
            }

            runCatching {
                requestAiSummary(todo)
            }.onSuccess {
                pollAndCacheAiSummary(todo, key, fallbackSuccess)
                return@launch
            }.onFailure { exception ->
                SentryExceptionReporter.capture(exception)
                Log.e(javaClass.name, "AI 요약 요청에 실패했습니다: ${todo.title}", exception)
                if (fallbackSuccess != null) {
                    return@launch
                }
            }

            val reportedTodoResult = runCatching {
                findReportedTodo(todo)
            }
            val reportedTodo = reportedTodoResult.getOrNull()
            reportedTodo?.toAiSummarySuccessOrNull()?.let { success ->
                cacheAndShowAiSummary(key, success)
                return@launch
            }

            if (reportedTodoResult.isFailure) {
                reportedTodoResult.exceptionOrNull()?.let(SentryExceptionReporter::capture)
                aiSummaryStates[key] = AiSummaryUiState.Error
                return@launch
            }

            if (reportedTodo?.isProvisional == true) {
                pollAndCacheAiSummary(todo, key)
                return@launch
            }

            if (reportedTodo?.isConfirmed == true) {
                aiSummaryStates[key] = AiSummaryUiState.Empty
                return@launch
            }

            aiSummaryStates[key] = AiSummaryUiState.Error
        }
    }

    suspend fun loadTodos(
        forceRefresh: Boolean = false,
        forceLogin: Boolean = false,
        allowRefresh: Boolean = true,
        showBlockingLoading: Boolean = true,
        source: RefreshSource = RefreshSource.APP_START,
    ): TodoData? {
        termSelectionStore.selectedTerm.value?.let { selectedTerm ->
            return loadSelectedTermTodos(
                term = selectedTerm,
                forceLogin = forceLogin,
                showBlockingLoading = showBlockingLoading,
            )
        }

        if(isLoading.value) {
            return null
        }

        isLoading.value = true
        loadingProgress.value = 0f
        showNetworkError.value = false

        return try {
            val cachedTodoData = mainRepository.getTodoData()
            val hasCachedTodoData = cachedTodoData.loadedAt.isNotEmpty()
            if(hasCachedTodoData) {
                updateTodoState(cachedTodoData)
            }

            if (!allowRefresh && hasCachedTodoData) {
                cachedTodoData
            } else if (!forceRefresh && !shouldRefreshOnOpen(cachedTodoData)) {
                cachedTodoData
            } else {
                showLoading.value = showBlockingLoading
                when (val refreshResult = lmsRefreshRepository.refreshTodos(
                    source = source,
                    forceLogin = forceLogin,
                    loadingState = {
                        viewModelScope.launch {
                            loadingProgress.value = it
                        }
                    }
                )) {
                    is TodoRefreshResult.Success -> {
                        loadingProgress.value = 1f
                        showNetworkError.value = false
                        updateTodoState(refreshResult.todoData)
                        refreshResult.todoData
                    }

                    is TodoRefreshResult.Skipped -> {
                        Log.i(javaClass.name, refreshResult.reason)
                        cachedTodoData.takeIf { hasCachedTodoData }
                    }

                    is TodoRefreshResult.Failure -> {
                        Log.e(javaClass.name, refreshResult.message, refreshResult.throwable)
                        showNetworkError.value = true
                        showNetworkCause.value = refreshResult.message
                        null
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            SentryExceptionReporter.capture(e)
            Log.e(javaClass.name, "과제 정보를 갱신하지 못했습니다.", e)
            showNetworkError.value = true
            showNetworkCause.value = e.localizedMessage ?: "알 수 없는 에러"
            null
        } finally {
            isLoading.value = false
            showLoading.value = false
        }
    }

    private suspend fun loadSelectedTermTodos(
        term: Term,
        forceLogin: Boolean = false,
        showBlockingLoading: Boolean = true,
    ): TodoData? {
        if (isLoading.value) {
            return null
        }

        isLoading.value = true
        loadingProgress.floatValue = 0f
        showLoading.value = showBlockingLoading
        showNetworkError.value = false

        return try {
            val todoData = lmsRefreshRepository.loadTodosForTerm(
                term = term,
                forceLogin = forceLogin,
                loadingState = { progress ->
                    loadingProgress.floatValue = progress
                },
            )
            if (termSelectionStore.selectedTerm.value?.id != term.id) {
                return null
            }

            loadingProgress.floatValue = 1f
            aiSummaryStates.clear()
            updateTodoState(todoData)
            todoData
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            SentryExceptionReporter.capture(e)
            Log.e(javaClass.name, "선택한 학기 정보를 불러오지 못했습니다.", e)
            showNetworkError.value = true
            showNetworkCause.value = e.localizedMessage ?: "알 수 없는 에러"
            null
        } finally {
            isLoading.value = false
            showLoading.value = false
        }
    }

    private fun updateTodoState(todoData: TodoData) {
        todos.apply {
            clear()
            addAll(todoData.todos)
        }

        submitted.apply {
            clear()
            addAll(todoData.submitted)
//            addAll(todoData.submitted.filterRecentlySubmitted())
        }

        loadedAt.value = todoData.loadedAt

        todoData.aiSummaryCache.forEach { (key, cache) ->
            cache.toAiSummarySuccessOrNull()?.let { success ->
                aiSummaryStates[key] = success
            }
        }
    }

    private suspend fun findReportedTodo(todo: TodoInfo): ReportedTodoResponse? =
        mainRepository.getReportedTodos()
            .firstOrNull { response -> response.todo.matches(todo) }
            ?.todo

    private suspend fun requestAiSummary(todo: TodoInfo) {
        val lmsSession = lmsRefreshRepository.getLmsSessionRequest()
        mainRepository.reportTodoWithAnalysis(
            todo = todo,
            lmsSession = lmsSession,
        )
    }

    private suspend fun pollAndCacheAiSummary(
        todo: TodoInfo,
        key: String,
        fallbackSuccess: AiSummaryUiState.Success? = null,
    ) {
        if (fallbackSuccess == null) {
            aiSummaryStates[key] = AiSummaryUiState.Analyzing
        }

        repeat(AI_SUMMARY_POLL_ATTEMPTS) { attempt ->
            val success = runCatching {
                findReportedTodo(todo)?.toAiSummarySuccessOrNull()
            }.getOrNull()

            if (success != null) {
                cacheAndShowAiSummary(key, success)
                return
            }

            if (attempt < AI_SUMMARY_POLL_ATTEMPTS - 1) {
                delay(AI_SUMMARY_POLL_INTERVAL_MILLIS)
            }
        }

        aiSummaryStates[key] = fallbackSuccess ?: AiSummaryUiState.Empty
    }

    private suspend fun cacheAndShowAiSummary(
        key: String,
        success: AiSummaryUiState.Success,
    ) {
        if (termSelectionStore.selectedTerm.value == null) {
            mainRepository.cacheAiSummary(
                key = key,
                summary = success.summary,
                estimatedDurationMinutes = success.estimatedDurationMinutes,
            )
        }
        aiSummaryStates[key] = success
    }

    private fun shouldRefreshOnOpen(todoData: TodoData): Boolean {
        if (todoData.submitted.any { it.submittedAt.isBlank() && it.todoId != -1 }) {
            return true
        }

        val loadedDate = todoData.loadedAt.toLocalDateOrNull()
            ?: return true
        val today = LocalDate.now(REFRESH_DATE_ZONE_ID)
        return loadedDate.isBefore(today)
    }

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching {
        Instant.parse(this)
            .atZone(REFRESH_DATE_ZONE_ID)
            .toLocalDate()
    }.getOrNull()

}

sealed interface AiSummaryUiState {
    data object Loading : AiSummaryUiState
    data object Analyzing : AiSummaryUiState
    data object Empty : AiSummaryUiState
    data class Success(
        val summary: String,
        val estimatedDurationMinutes: Int?,
    ) : AiSummaryUiState
    data object Error : AiSummaryUiState
}

fun TodoInfo.aiSummaryKey(): String =
    "${subject?.id ?: subjectId}:$todoId:${type.name}"

fun TodoInfo.canRequestAiSummary(): Boolean =
    type == TodoType.ASSIGNMENT && description.isNotBlank()

private fun ReportedTodoResponse.toAiSummarySuccessOrNull(): AiSummaryUiState.Success? {
    val summary = aiSummary.orEmpty().trim()
        .takeIf { isConfirmed && it.isNotBlank() }
        ?: return null

    return AiSummaryUiState.Success(
        summary = summary,
        estimatedDurationMinutes = estimatedDurationMinutes,
    )
}

private fun AiSummaryCache.toAiSummarySuccessOrNull(): AiSummaryUiState.Success? =
    summary.trim()
        .takeIf { it.isNotBlank() }
        ?.let { summary ->
            AiSummaryUiState.Success(
                summary = summary,
                estimatedDurationMinutes = estimatedDurationMinutes,
            )
        }

private val ReportedTodoResponse.isProvisional: Boolean
    get() = status.equals(TODO_STATUS_PROVISIONAL, ignoreCase = true)

private val ReportedTodoResponse.isConfirmed: Boolean
    get() = status.equals(TODO_STATUS_CONFIRMED, ignoreCase = true)

private fun List<TodoInfo>.filterRecentlySubmitted(
    now: Instant = Instant.now(),
): List<TodoInfo> = filter { todo ->
    if (todo.type != TodoType.SUBMITTED && todo.type != TodoType.SUBMITTED_LATE) {
        return@filter false
    }
    val submittedAt = todo.submittedAt.toInstantOrNull() ?: return@filter false
    Duration.between(submittedAt, now) <= SUBMITTED_VISIBLE_WINDOW
}

private fun String.toInstantOrNull(): Instant? = runCatching {
    Instant.parse(this)
}.getOrNull()
