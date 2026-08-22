package com.yourssu.ssutime.v2.screen.main

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AlertData
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.ExperimentalTime

private val REFRESH_DATE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
private val SUBMITTED_VISIBLE_WINDOW: Duration = Duration.ofHours(24)

@OptIn(ExperimentalTime::class)
class MainViewModel(
    private val mainRepository: MainRepository,
    private val lmsRefreshRepository: LmsRefreshRepository,
    private val termSelectionStore: TermSelectionStore,
) : ViewModel() {
    var todos = mutableStateListOf<TodoInfo>()
    var submitted = mutableStateListOf<TodoInfo>()
    var subjects = mutableStateListOf<SubjectInfo>()
    var isLoading = mutableStateOf(false)
    var showLoading = mutableStateOf(false)
    var loadingProgress = mutableFloatStateOf(0f)
    var loadedAt = mutableStateOf("")
    var showNetworkError = mutableStateOf(false)
    var showNetworkCause = mutableStateOf("")
    var showWidgetBadge = mutableStateOf(false)
    var onboardingInitialRefreshInProgress = mutableStateOf(false)
    var requiredShowAlertBottomSheet = mutableStateOf(false)
    private var handledHomeEntryVersion: Int? = null

    val unreadNoticeCount: Int
        get() = subjects.flatMap { it.discussions }.count {
            it.readState.equals("unread", ignoreCase = true)
        }

    init {
        viewModelScope.launch {
            val alertData = mainRepository.getAlertData()
            requiredShowAlertBottomSheet.value = shouldShowCallingAlertBottomSheet(alertData)
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
        val currentPeriod = callAlertRemindPeriod()
        val dataToSave = if (currentPeriod != null) {
            alertData.copy(lastCallAlertRemindPeriod = currentPeriod)
        } else {
            alertData
        }
        viewModelScope.launch {
            mainRepository.updateAlertData(dataToSave)
        }
        requiredShowAlertBottomSheet.value = false
    }

    fun showNetworkErrorScreen() {
        showNetworkError.value = true
    }

    fun dismissWidgetHelperBadge() {
        showWidgetBadge.value = false
        viewModelScope.launch {
            val alertData = mainRepository.getAlertData()
            mainRepository.updateAlertData(alertData.copy(showWidgetHelperBadge = false))
        }
    }

    fun markDiscussionAsRead(discussionId: Int) {
        val updatedList = subjects.map { subject ->
            val updatedDiscussions = subject.discussions.map { discussion ->
                if (discussion.id == discussionId) {
                    discussion.copy(readState = "read")
                } else {
                    discussion
                }
            }
            subject.copy(discussions = updatedDiscussions)
        }
        subjects.clear()
        subjects.addAll(updatedList)

        viewModelScope.launch {
            mainRepository.markDiscussionAsRead(discussionId)
        }
    }

    fun startInitialLoad(
        homeEntryVersion: Int,
        forceRefresh: Boolean = false,
        forceLogin: Boolean = false,
        allowRefresh: Boolean = true,
        showBlockingLoading: Boolean = true,
        source: RefreshSource = RefreshSource.APP_START,
        onSuccess: (TodoData) -> Unit = {},
    ) {
        if (!shouldRunInitialLoad(homeEntryVersion)) {
            return
        }

        viewModelScope.launch {
            val todoData = loadTodos(
                forceRefresh = forceRefresh,
                forceLogin = forceLogin,
                allowRefresh = allowRefresh,
                showBlockingLoading = showBlockingLoading,
                source = source,
            )
            if (todoData != null && !showNetworkError.value) {
                onSuccess(todoData)
            }
        }
    }

    suspend fun loadTodos(
        forceRefresh: Boolean = false,
        forceLogin: Boolean = false,
        allowRefresh: Boolean = true,
        showBlockingLoading: Boolean = true,
        source: RefreshSource = RefreshSource.FOREGROUND,
    ): TodoData? {
        if (isLoading.value) {
            return null
        }

        isLoading.value = true
        loadingProgress.value = 0f
        showLoading.value = showBlockingLoading
        showNetworkError.value = false

        return try {
            val cachedTodoData = mainRepository.getTodoData()
            val hasCachedTodoData = cachedTodoData.todos.isNotEmpty() || cachedTodoData.submitted.isNotEmpty()
            val shouldRefresh = allowRefresh && (forceRefresh || shouldRefreshOnOpen(cachedTodoData))

            if (!shouldRefresh) {
                updateTodoState(cachedTodoData)
                cachedTodoData
            } else {
                when (val refreshResult = lmsRefreshRepository.refreshTodos(
                    source = source,
                    forceLogin = forceLogin,
                    loadingState = { progress ->
                        loadingProgress.value = progress
                    },
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
        }

        subjects.apply {
            clear()
            addAll(todoData.subjects)
        }

        loadedAt.value = todoData.loadedAt
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

    private fun List<TodoInfo>.filterRecentlySubmitted(): List<TodoInfo> = filter { todo ->
        val submittedInstant = todo.submittedAt.toInstantOrNull() ?: return@filter false
        val threshold = Instant.now().minus(SUBMITTED_VISIBLE_WINDOW)
        submittedInstant.isAfter(threshold)
    }

    private fun String.toInstantOrNull(): Instant? = runCatching {
        Instant.parse(this)
    }.getOrNull()
}
