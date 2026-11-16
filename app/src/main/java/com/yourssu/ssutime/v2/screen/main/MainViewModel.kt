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
import com.yourssu.ssutime.v2.todo.TODO_DEADLINE_ZONE_ID
import com.yourssu.ssutime.v2.todo.sortedBySubmittedAtDescending
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import kotlin.time.ExperimentalTime

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
    var isCallAlertRepopup = mutableStateOf(false)
    var isEnableSubmittedFile = mutableStateOf(false)
    private var handledHomeEntryVersion: Int? = null

    val unreadNoticeCount: Int
        get() = subjects.flatMap { it.discussions }.count {
            it.readState.equals("unread", ignoreCase = true)
        }

    init {
        viewModelScope.launch {
            mainRepository.labsData.collect { labsData ->
                isEnableSubmittedFile.value = labsData.isEnableSubmittedFile
            }
        }

        viewModelScope.launch {
            val alertData = mainRepository.getAlertData()
            val shouldShow = shouldShowCallingAlertBottomSheet(alertData)
            requiredShowAlertBottomSheet.value = shouldShow
            isCallAlertRepopup.value = shouldShow && alertData.valid
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

        viewModelScope.launch {
            lmsRefreshRepository.onboardingRefreshError.collect { errorMessage ->
                if (!errorMessage.isNullOrBlank() && loadedAt.value.isBlank()) {
                    showNetworkCause.value = errorMessage
                    showNetworkError.value = true
                }
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
        isCallAlertRepopup.value = false
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
    ) {
        if (!shouldRunInitialLoad(homeEntryVersion)) {
            return
        }

        viewModelScope.launch {
            loadTodos(
                forceRefresh = forceRefresh,
                forceLogin = forceLogin,
                allowRefresh = allowRefresh,
                showBlockingLoading = showBlockingLoading,
                source = source,
            )
        }
    }

    suspend fun loadTodos(
        forceRefresh: Boolean = false,
        forceLogin: Boolean = false,
        allowRefresh: Boolean = true,
        showBlockingLoading: Boolean = true,
        source: RefreshSource = RefreshSource.FOREGROUND,
    ): TodoData? = runTodoLoad(
        showBlockingLoading = showBlockingLoading,
        errorLogMessage = "과제 정보를 갱신하지 못했습니다.",
    ) {
        val cachedTodoData = mainRepository.getTodoData()
        val hasCachedTodoData = cachedTodoData.todos.isNotEmpty() || cachedTodoData.submitted.isNotEmpty()
        val shouldRefresh = allowRefresh && (forceRefresh || shouldRefreshOnOpen(cachedTodoData))

        if (!shouldRefresh) {
            if (!allowRefresh) {
                lmsRefreshRepository.awaitOnboardingInitialRefreshIfRunning()
            }
            val latestTodoData = mainRepository.getTodoData()
            val onboardingError = lmsRefreshRepository.consumeOnboardingRefreshError()
            if (latestTodoData.loadedAt.isBlank() && !onboardingError.isNullOrBlank()) {
                showNetworkCause.value = onboardingError
                showNetworkError.value = true
                null
            } else {
                updateTodoState(latestTodoData)
                latestTodoData
            }
        } else {
            when (val refreshResult = lmsRefreshRepository.refreshTodos(
                source = source,
                forceLogin = forceLogin,
                loadingState = { progress ->
                    loadingProgress.floatValue = progress
                },
            )) {
                is TodoRefreshResult.Success -> {
                    loadingProgress.floatValue = 1f
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
    }

    private suspend fun loadSelectedTermTodos(
        term: Term,
        forceLogin: Boolean = false,
        showBlockingLoading: Boolean = true,
    ): TodoData? = runTodoLoad(
        showBlockingLoading = showBlockingLoading,
        errorLogMessage = "선택한 학기 정보를 불러오지 못했습니다.",
    ) {
        val todoData = lmsRefreshRepository.loadTodosForTerm(
            term = term,
            forceLogin = forceLogin,
            loadingState = { progress ->
                loadingProgress.floatValue = progress
            },
        )
        if (termSelectionStore.selectedTerm.value?.id != term.id) {
            return@runTodoLoad null
        }

        loadingProgress.floatValue = 1f
        updateTodoState(todoData)
        todoData
    }

    private suspend fun runTodoLoad(
        showBlockingLoading: Boolean,
        errorLogMessage: String,
        block: suspend () -> TodoData?,
    ): TodoData? {
        if (isLoading.value) {
            return null
        }

        isLoading.value = true
        loadingProgress.floatValue = 0f
        showLoading.value = showBlockingLoading
        showNetworkError.value = false

        return try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            SentryExceptionReporter.capture(e)
            Log.e(javaClass.name, errorLogMessage, e)
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
            addAll(todoData.submitted.sortedBySubmittedAtDescending())
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
        val today = LocalDate.now(TODO_DEADLINE_ZONE_ID)
        return loadedDate.isBefore(today)
    }

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching {
        Instant.parse(this)
            .atZone(TODO_DEADLINE_ZONE_ID)
            .toLocalDate()
    }.getOrNull()
}
