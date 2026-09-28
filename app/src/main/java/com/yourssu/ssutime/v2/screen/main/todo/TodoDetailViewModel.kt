package com.yourssu.ssutime.v2.screen.main.todo

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AiSummaryCache
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.network.AssignmentAnalysisResponse
import com.yourssu.data.network.AttachmentLinkResponse
import com.yourssu.data.network.ReportedTodoResponse
import com.yourssu.data.network.hasNoAnalyzableAttachment
import com.yourssu.data.network.isFailed
import com.yourssu.data.network.isSkipped
import com.yourssu.data.network.isSuccessful
import com.yourssu.data.network.matches
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import com.yourssu.ssutime.v2.screen.main.TermSelectionStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

private const val AI_SUMMARY_POLL_ATTEMPTS = 30
private const val AI_SUMMARY_POLL_INTERVAL_MILLIS = 2_000L
private const val TODO_STATUS_PROVISIONAL = "PROVISIONAL"
private const val TODO_STATUS_CONFIRMED = "CONFIRMED"

@OptIn(ExperimentalTime::class)
class TodoDetailViewModel(
    private val mainRepository: MainRepository,
    private val lmsRefreshRepository: LmsRefreshRepository,
    private val termSelectionStore: TermSelectionStore,
) : ViewModel() {
    var aiSummaryState by mutableStateOf<AiSummaryUiState?>(null)
        private set

    private var aiSummaryJob: Job? = null
    private var activeTodoKey: String? = null

    fun hideTodo(todo: TodoInfo, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            mainRepository.hideTodo(todo)
            onCompleted()
        }
    }

    fun loadAiSummary(todo: TodoInfo) {
        val key = todo.aiSummaryKey()
        if (activeTodoKey == key && aiSummaryJob?.isActive == true) {
            return
        }

        aiSummaryJob?.cancel()
        activeTodoKey = key

        if (!todo.canRequestAiSummary()) {
            Log.i(
                javaClass.name,
                "AI summary request skipped: type=${todo.type}, hasDescription=${todo.description.isNotBlank()}",
            )
            aiSummaryState = AiSummaryUiState.Empty
            return
        }

        aiSummaryJob = viewModelScope.launch {
            val fallbackSuccess = mainRepository.getCachedAiSummary(key)
                ?.toAiSummarySuccessOrNull()

            aiSummaryState = fallbackSuccess ?: AiSummaryUiState.Loading

            val analysisResult = runCatching {
                requestAiSummary(todo)
            }

            if (analysisResult.isFailure) {
                val exception = analysisResult.exceptionOrNull()
                if (exception != null) {
                    SentryExceptionReporter.capture(exception)
                    Log.e(javaClass.name, "AI 요약 요청에 실패했습니다: ${todo.title}", exception)
                }
                if (fallbackSuccess != null) {
                    return@launch
                }
            } else {
                val analysisResponse = analysisResult.getOrNull()
                if (analysisResponse?.hasNoAnalyzableAttachment == true) {
                    Log.i(javaClass.name, "분석 가능한 첨부파일이 없습니다: ${todo.title}")
                    val reportedTodo = runCatching { findReportedTodo(todo) }.getOrNull()
                    reportedTodo?.toAiSummarySuccessOrNull()?.let { success ->
                        cacheAndShowAiSummary(key, success)
                        return@launch
                    }
                    aiSummaryState = fallbackSuccess ?: AiSummaryUiState.Empty
                    return@launch
                }

                if (analysisResponse?.isSuccessful == true) {
                    pollReportedTodoAndCache(todo, key, fallbackSuccess)
                    return@launch
                }
                if (analysisResponse?.isFailed == true) {
                    aiSummaryState = fallbackSuccess ?: AiSummaryUiState.Error
                    return@launch
                }
                if (analysisResponse?.isSkipped == true) {
                    aiSummaryState = fallbackSuccess ?: AiSummaryUiState.Empty
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
                aiSummaryState = AiSummaryUiState.Error
                return@launch
            }

            if (reportedTodo?.isProvisional == true) {
                pollReportedTodoAndCache(todo, key)
                return@launch
            }

            aiSummaryState = if (reportedTodo?.isConfirmed == true) {
                AiSummaryUiState.Empty
            } else {
                AiSummaryUiState.Error
            }
        }
    }

    private suspend fun findReportedTodo(todo: TodoInfo): ReportedTodoResponse? =
        mainRepository.getReportedTodos()
            .firstOrNull { response -> response.todo.matches(todo) }
            ?.todo

    private suspend fun requestAiSummary(todo: TodoInfo): AssignmentAnalysisResponse? {
        val lmsSession = lmsRefreshRepository.getLmsSessionRequest()
        return mainRepository.reportTodoWithAnalysis(
            todo = todo,
            lmsSession = lmsSession,
        )
    }

    private suspend fun pollReportedTodoAndCache(
        todo: TodoInfo,
        key: String,
        fallbackSuccess: AiSummaryUiState.Success? = null,
    ) {
        if (fallbackSuccess == null) {
            aiSummaryState = AiSummaryUiState.Analyzing
        }
        var lastReportedTodo: ReportedTodoResponse? = null

        repeat(AI_SUMMARY_POLL_ATTEMPTS) { attempt ->
            val reportedTodo = runCatching {
                findReportedTodo(todo)
            }.onFailure { exception ->
                if (attempt == 0) {
                    Log.w(javaClass.name, "AI 요약이 포함된 할 일 조회에 실패했습니다.", exception)
                }
            }.getOrNull()
            lastReportedTodo = reportedTodo

            if (attempt == 0) {
                Log.i(
                    javaClass.name,
                    "AI summary todo lookup: matched=${reportedTodo != null}, " +
                        "todoStatus=${reportedTodo?.status}, " +
                        "summaryLength=${reportedTodo?.aiSummary.orEmpty().length}",
                )
            }

            val success = reportedTodo?.toAiSummarySuccessOrNull()
            if (success != null) {
                cacheAndShowAiSummary(key, success)
                return
            }

            if (attempt < AI_SUMMARY_POLL_ATTEMPTS - 1) {
                delay(AI_SUMMARY_POLL_INTERVAL_MILLIS)
            }
        }

        aiSummaryState = fallbackSuccess ?: if (lastReportedTodo?.isConfirmed == true) {
            AiSummaryUiState.Empty
        } else {
            AiSummaryUiState.Error
        }
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
                attachmentLinks = success.attachmentLinks,
            )
        }
        aiSummaryState = success
    }
}

sealed interface AiSummaryUiState {
    data object Loading : AiSummaryUiState
    data object Analyzing : AiSummaryUiState
    data object Empty : AiSummaryUiState
    data class Success(
        val summary: String,
        val estimatedDurationMinutes: Int?,
        val attachmentLinks: List<AttachmentLinkResponse> = emptyList(),
    ) : AiSummaryUiState
    data object Error : AiSummaryUiState
}

fun TodoInfo.aiSummaryKey(): String =
    "${subject?.id ?: subjectId}:$todoId:${type.name}"

fun TodoInfo.canRequestAiSummary(): Boolean =
    type == TodoType.ASSIGNMENT && description.isNotBlank()

private fun ReportedTodoResponse.toAiSummarySuccessOrNull(): AiSummaryUiState.Success? {
    val summary = aiSummary.orEmpty().trim()
        .takeIf { it.isNotBlank() }
        ?: return null

    return AiSummaryUiState.Success(
        summary = summary,
        estimatedDurationMinutes = estimatedDurationMinutes,
        attachmentLinks = attachmentLinks,
    )
}

private fun AiSummaryCache.toAiSummarySuccessOrNull(): AiSummaryUiState.Success? =
    summary.trim()
        .takeIf { it.isNotBlank() }
        ?.let { summary ->
            AiSummaryUiState.Success(
                summary = summary,
                estimatedDurationMinutes = estimatedDurationMinutes,
                attachmentLinks = attachmentLinks,
            )
        }

private val ReportedTodoResponse.isProvisional: Boolean
    get() = status.equals(TODO_STATUS_PROVISIONAL, ignoreCase = true)

private val ReportedTodoResponse.isConfirmed: Boolean
    get() = status.equals(TODO_STATUS_CONFIRMED, ignoreCase = true)
