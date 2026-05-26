package com.yourssu.ssutime.v2.screen.main

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AlertData
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.network.matches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val REFRESH_DATE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
private val SUBMITTED_VISIBLE_WINDOW: Duration = Duration.ofHours(24)

class MainViewModel(
    private val mainRepository: MainRepository,
    private val lmsRefreshRepository: LmsRefreshRepository,
) : ViewModel() {
    var todos = mutableStateListOf<TodoInfo>()
    var submitted = mutableStateListOf<TodoInfo>()
    var isLoading = mutableStateOf(false)
    var showLoading = mutableStateOf(false)
    var loadingProgress = mutableFloatStateOf(0f)
    var loadedAt = mutableStateOf("")
    var showNetworkError = mutableStateOf(false)
    var showWidgetBadge = mutableStateOf(false)
    val aiSummaryStates = mutableStateMapOf<String, AiSummaryUiState>()

    var requiredShowAlertBottomSheet = mutableStateOf(false)

    init {
        viewModelScope.launch {
            val alertData = mainRepository.getAlertData()
            requiredShowAlertBottomSheet.value = !alertData.valid
            showWidgetBadge.value = alertData.showWidgetHelperBadge
        }
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
        if (aiSummaryStates[key] == AiSummaryUiState.Loading) {
            return
        }

        viewModelScope.launch {
            val analysisAlreadyRequested = aiSummaryStates[key] == AiSummaryUiState.Empty
            aiSummaryStates[key] = AiSummaryUiState.Loading

            val reportedTodoResult = runCatching {
                mainRepository.getReportedTodos()
                    .firstOrNull { response -> response.todo.matches(todo) }
                    ?.todo
            }

            val reportedTodo = reportedTodoResult.getOrNull()
            if (reportedTodo != null) {
                val aiSummary = reportedTodo.aiSummary.orEmpty()
                if (aiSummary.isNotBlank()) {
                    aiSummaryStates[key] =
                        AiSummaryUiState.Success(aiSummary)
                    return@launch
                }

                if (analysisAlreadyRequested) {
                    aiSummaryStates[key] = AiSummaryUiState.Empty
                    return@launch
                }
            }

            if (analysisAlreadyRequested && reportedTodoResult.isFailure) {
                aiSummaryStates[key] = AiSummaryUiState.Error
                return@launch
            }

            runCatching {
                val lmsSession = lmsRefreshRepository.getLmsSessionRequest()
                mainRepository.reportTodoWithAnalysis(
                    todo = todo,
                    lmsSession = lmsSession,
                )
            }.onSuccess {
                val aiSummary = runCatching {
                    mainRepository.getReportedTodos()
                        .firstOrNull { response -> response.todo.matches(todo) }
                        ?.todo
                        ?.aiSummary
                        .orEmpty()
                }.getOrDefault("")

                aiSummaryStates[key] = if (aiSummary.isNotBlank()) {
                    AiSummaryUiState.Success(aiSummary)
                } else {
                    AiSummaryUiState.Empty
                }
            }.onFailure { exception ->
                Log.e(javaClass.name, "AI 요약 요청에 실패했습니다: ${todo.title}", exception)
                aiSummaryStates[key] = AiSummaryUiState.Error
            }
        }
    }

    suspend fun loadTodos(forceRefresh: Boolean = false, allowRefresh: Boolean = true) {
        if(isLoading.value) {
            return
        }

        isLoading.value = true
        loadingProgress.value = 0f
        showNetworkError.value = false

        try {
            val cachedTodoData = mainRepository.getTodoData()
            val hasCachedTodoData = cachedTodoData.loadedAt.isNotEmpty()
            if(hasCachedTodoData) {
                updateTodoState(cachedTodoData)
            }

            if (!allowRefresh) {
                return
            }

            if (!forceRefresh && !shouldRefreshOnOpen(cachedTodoData)) {
                return
            }

            showLoading.value = true
            when (val refreshResult = lmsRefreshRepository.refreshTodos(
                source = RefreshSource.MANUAL,
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
                }

                is TodoRefreshResult.Skipped -> {
                    Log.i(javaClass.name, refreshResult.reason)
                }

                is TodoRefreshResult.Failure -> {
                    Log.e(javaClass.name, refreshResult.message, refreshResult.throwable)
                    showNetworkError.value = true
                }
            }
        } catch(e: Exception) {
            if(e is CancellationException) throw e
            Log.e(javaClass.name, "과제 정보를 갱신하지 못했습니다.", e)
            showNetworkError.value = true
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
            addAll(todoData.submitted.filterRecentlySubmitted())
        }

        loadedAt.value = todoData.loadedAt
    }

    private fun shouldRefreshOnOpen(todoData: TodoData): Boolean {
        if (todoData.submitted.any { it.submittedAt.isBlank() }) {
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
    data object Empty : AiSummaryUiState
    data class Success(val summary: String) : AiSummaryUiState
    data object Error : AiSummaryUiState
}

fun TodoInfo.aiSummaryKey(): String =
    "${subject?.id ?: subjectId}:$todoId:${type.name}"

fun TodoInfo.canRequestAiSummary(): Boolean =
    type == TodoType.ASSIGNMENT && description.isNotBlank()

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
